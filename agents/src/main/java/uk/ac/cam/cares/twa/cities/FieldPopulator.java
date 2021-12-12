package uk.ac.cam.cares.twa.cities;

import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;

import java.io.InvalidClassException;
import java.lang.reflect.*;
import java.net.URI;
import java.util.List;
import java.util.function.*;

class InstantiationParameters {
  public final KnowledgeBaseClientInterface kgClient;
  public final int recursiveInstantiationDepth;

  public InstantiationParameters(KnowledgeBaseClientInterface kgClient, int recursiveInstantiationDepth) {
    this.kgClient = kgClient;
    this.recursiveInstantiationDepth = recursiveInstantiationDepth;
  }
}

public class FieldPopulator {
  public final Field field;
  public final Method getter;
  public final Method setter;
  private final BiFunction<JSONObject, InstantiationParameters, Object> parser;
  private final BiConsumer<Object, Object> pusher;
  private final Supplier<Object> defaultConstructor;
  public FieldPopulator(Field field) throws NoSuchMethodException, InvalidClassException {
    this.field = field;
    // Determine characteristics of field
    Class<?> parentType = field.getDeclaringClass();
    Class<?> outerType = field.getType();
    boolean isList = List.class.isAssignableFrom(outerType);
    Class<?> innerType = isList ? field.getAnnotation(ModelField.class).innerType() : outerType;
    boolean isModel = Model.class.isAssignableFrom(innerType);
    // Get the lombok accessors/modifiers --- we can't access private/protected fields. :(
    String fieldName = field.getName();
    fieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    getter = parentType.getMethod("get" + fieldName);
    setter = parentType.getMethod("set" + fieldName, outerType);
    Supplier<Object> tentativeDefaultConstructor;
    // Generate parser
    if (isModel) {
      Constructor<?> constructor = innerType.getConstructor();
      parser = (JSONObject row, InstantiationParameters params) -> {
        try {
          Model model = (Model) constructor.newInstance();
          if (params.recursiveInstantiationDepth > 0)
            model.populateAll(row.getString(Model.VALUE), params.kgClient, params.recursiveInstantiationDepth - 1);
          else
            model.setIri(URI.create(row.getString(Model.VALUE)));
          return model;
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }
      };
      tentativeDefaultConstructor = () -> null;
    } else if (innerType == int.class) {
      parser = (JSONObject row, InstantiationParameters instantiationParams) -> row.getInt(Model.VALUE);
      tentativeDefaultConstructor = () -> 0;
    } else if (innerType == double.class) {
      parser = (JSONObject row, InstantiationParameters instantiationParams) -> row.getDouble(Model.VALUE);
      tentativeDefaultConstructor = () -> 0;
    } else if (innerType == String.class) {
      parser = (JSONObject row, InstantiationParameters instantiationParams) -> row.getString(Model.VALUE);
      tentativeDefaultConstructor = () -> null;
    } else if (innerType == URI.class) {
      parser = (JSONObject row, InstantiationParameters instantiationParams) -> URI.create(row.getString(Model.VALUE));
      tentativeDefaultConstructor = () -> null;
    } else {
      throw new InvalidClassException(innerType.toString(), "Class not supported.");
    }
    // Generate pusher
    if (isList) {
      // Due to type erasure, ArrayLists accept Objects, not innerTypes.
      Method adder = outerType.getMethod("add", Object.class);
      pusher = (Object object, Object value) -> {
        try {
          Object list = getter.invoke(object);
          adder.invoke(list, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
          e.printStackTrace();
        }
      };
      // Override previously assigned default constructor, which was for innerType
      Constructor<?> constructor = outerType.getConstructor();
      tentativeDefaultConstructor = () -> {
        try {
          return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
          e.printStackTrace();
          return null;
        }
      };
    } else {
      pusher = (Object object, Object value) -> {
        try {
          setter.invoke(object, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
          e.printStackTrace();
        }
      };
    }
    defaultConstructor = tentativeDefaultConstructor;
    System.err.println("innerType: " + innerType + ", outerType: " + outerType + ", isList: " + isList + ", isModel: " + isModel);
  }

  /**
   * Populates the associated field of the object with the value in the row.
   */
  public void populate(Object object, JSONObject row, KnowledgeBaseClientInterface kgClient, int recursiveInstantiationDepth) {
    pusher.accept(object, parser.apply(row, new InstantiationParameters(kgClient, recursiveInstantiationDepth)));
  }

  /**
   * Clears the associated field of the object with the value in the row.
   */
  public void clear(Object object) {
    try {
      setter.invoke(object, defaultConstructor.get());
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
  }

}