package uk.ac.cam.cares.twa.cities.models;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import lombok.Getter;
import lombok.Setter;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

/**
 * {@link Model} is the abstract base for classes which represent objects in Blazegraph. It implements a number of
 * methods which push data to and pull data from Blazegraph, and also maintains automatic dirty tracking of field values
 * to only write updates for modified fields on push. Also see {@link MetaModel}.
 * <p>
 * Subclasses of {@link Model} should annotate fields meant for database read-write with {@link Getter},
 * {@link Setter} and {@link FieldAnnotation}.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
public abstract class Model {

  enum SpecialFieldInstruction {
    UNSET,
    UNPULLED,
    FORCE_PUSH
  }

  @Getter String iri;
  @Getter ModelContext context;
  final MetaModel metaModel;
  boolean deleted;

  // Minimised copies of field values at the last synchronisation with the database, indexed by FieldInterface.index.
  final Object[] cleanValues;

  /**
   * The direct Model constructor is for {@link ModelContext} internal use only. To create a model, use one of the
   * factory functions in {@link ModelContext}.
   */
  public Model() {
    metaModel = MetaModel.get(this.getClass());
    cleanValues = new Object[metaModel.fieldMap.size()];
    // Initialise lists to empty lists if they haven't already been initialised
    for (Map.Entry<FieldKey, FieldInterface> vectorEntry : metaModel.vectorFieldList) {
      try {
        if (vectorEntry.getValue().getter.invoke(this) == null)
          vectorEntry.getValue().clear(this);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new JPSRuntimeException(e);
      }
    }
  }

  /**
   * Clears all field of the instance
   */
  public void clearAll() {
    for (FieldInterface field : metaModel.fieldMap.values())
      field.clear(this);
  }

  /**
   * Makes all fields of the instance clean, so they will not be written on push unless changed again.
   */
  public void setAllClean() {
    for (FieldInterface field : metaModel.fieldMap.values())
      cleanValues[field.index] = field.getMinimised(this);
  }

  /**
   * Makes all fields of the instance dirty, so they will be written on push.
   */
  public void setAllDirty() {
    Arrays.fill(cleanValues, SpecialFieldInstruction.FORCE_PUSH);
  }

  /**
   * Wraps {@link ModelContext#pushChanges(Model)}.
   */
  public void pushChanges() {
    context.pushChanges(this);
  }

  /**
   * Wraps {@link ModelContext#delete(Model)}.
   */
  public void delete() {
    context.delete(this);
  }

  /**
   * Wraps {@link ModelContext#recursivePullAll(Model, int)}.
   */
  public void recursivePullAll(int recursionRadius) {
    context.recursivePullAll(this, recursionRadius);
  }

  /**
   * Wraps {@link ModelContext#pullAll(Model)}.
   */
  public void pullAll() {
    context.pullAll(this);
  }


  /**
   * Wraps {@link ModelContext#recursivePullPartial(Model, int, String...)}.
   */
  public void recursivePull(int recursionRadius, String... fieldNames) {
    context.recursivePullPartial(this, recursionRadius, fieldNames);
  }

  /**
   * Wraps {@link ModelContext#pullPartial(Model, String...)}.
   */
  public void pull(String... fieldNames) {
    context.pullPartial(this, fieldNames);
  }

  /**
   * Wraps {@link ModelContext#pullScalars(Model, String...)}.
   */
  public void pullScalars(String... fieldNames) {
    context.pullScalars(this, fieldNames);
  }

  /**
   * Wraps @link ModelContext#pullVectors(Model, String...)}.
   */
  public void pullVectors(String... fieldNames) {
    context.pullVectors(this, fieldNames);
  }

  /**
   * Equality is defined as all {@link Model}-managed fields matching. <code>originalFieldValues</code> and fields
   * without {@link FieldAnnotation} annotations do not have to match.
   * @param o the object to compare against.
   * @return whether the objects are equal.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    for (FieldInterface field : metaModel.fieldMap.values())
      if (!field.equals(this, (Model) o))
        return false;
    return true;
  }

}
