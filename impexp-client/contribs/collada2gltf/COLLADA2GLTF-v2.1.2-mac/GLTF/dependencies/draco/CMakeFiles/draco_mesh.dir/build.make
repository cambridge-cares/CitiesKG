# CMAKE generated file: DO NOT EDIT!
# Generated by "Unix Makefiles" Generator, CMake Version 3.11

# Delete rule output on recipe failure.
.DELETE_ON_ERROR:


#=============================================================================
# Special targets provided by cmake.

# Disable implicit rules so canonical targets will work.
.SUFFIXES:


# Remove some rules from gmake that .SUFFIXES does not remove.
SUFFIXES =

.SUFFIXES: .hpux_make_needs_suffix_list


# Suppress display of executed commands.
$(VERBOSE).SILENT:


# A target that is always out of date.
cmake_force:

.PHONY : cmake_force

#=============================================================================
# Set environment variables for the build.

# The shell in which to execute make rules.
SHELL = /bin/sh

# The CMake executable.
CMAKE_COMMAND = /usr/local/Cellar/cmake/3.11.4/bin/cmake

# The command to remove a file.
RM = /usr/local/Cellar/cmake/3.11.4/bin/cmake -E remove -f

# Escaping for special characters.
EQUALS = =

# The top-level source directory on which CMake was run.
CMAKE_SOURCE_DIR = /Users/sonnguyen/Downloads/COLLADA2GLTF

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = /Users/sonnguyen/Downloads/COLLADA2GLTF/build

# Include any dependencies generated for this target.
include GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/depend.make

# Include the progress variables for this target.
include GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/progress.make

# Include the compile flags for this target's objects.
include GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/flags.make

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/corner_table.cc.o: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/flags.make
GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/corner_table.cc.o: ../GLTF/dependencies/draco/src/draco/mesh/corner_table.cc
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/sonnguyen/Downloads/COLLADA2GLTF/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Building CXX object GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/corner_table.cc.o"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/draco_mesh.dir/src/draco/mesh/corner_table.cc.o -c /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/corner_table.cc

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/corner_table.cc.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/draco_mesh.dir/src/draco/mesh/corner_table.cc.i"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/corner_table.cc > CMakeFiles/draco_mesh.dir/src/draco/mesh/corner_table.cc.i

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/corner_table.cc.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/draco_mesh.dir/src/draco/mesh/corner_table.cc.s"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/corner_table.cc -o CMakeFiles/draco_mesh.dir/src/draco/mesh/corner_table.cc.s

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh.cc.o: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/flags.make
GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh.cc.o: ../GLTF/dependencies/draco/src/draco/mesh/mesh.cc
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/sonnguyen/Downloads/COLLADA2GLTF/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_2) "Building CXX object GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh.cc.o"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh.cc.o -c /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh.cc

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh.cc.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh.cc.i"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh.cc > CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh.cc.i

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh.cc.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh.cc.s"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh.cc -o CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh.cc.s

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_are_equivalent.cc.o: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/flags.make
GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_are_equivalent.cc.o: ../GLTF/dependencies/draco/src/draco/mesh/mesh_are_equivalent.cc
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/sonnguyen/Downloads/COLLADA2GLTF/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_3) "Building CXX object GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_are_equivalent.cc.o"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_are_equivalent.cc.o -c /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_are_equivalent.cc

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_are_equivalent.cc.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_are_equivalent.cc.i"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_are_equivalent.cc > CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_are_equivalent.cc.i

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_are_equivalent.cc.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_are_equivalent.cc.s"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_are_equivalent.cc -o CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_are_equivalent.cc.s

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_attribute_corner_table.cc.o: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/flags.make
GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_attribute_corner_table.cc.o: ../GLTF/dependencies/draco/src/draco/mesh/mesh_attribute_corner_table.cc
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/sonnguyen/Downloads/COLLADA2GLTF/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_4) "Building CXX object GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_attribute_corner_table.cc.o"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_attribute_corner_table.cc.o -c /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_attribute_corner_table.cc

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_attribute_corner_table.cc.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_attribute_corner_table.cc.i"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_attribute_corner_table.cc > CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_attribute_corner_table.cc.i

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_attribute_corner_table.cc.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_attribute_corner_table.cc.s"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_attribute_corner_table.cc -o CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_attribute_corner_table.cc.s

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_cleanup.cc.o: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/flags.make
GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_cleanup.cc.o: ../GLTF/dependencies/draco/src/draco/mesh/mesh_cleanup.cc
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/sonnguyen/Downloads/COLLADA2GLTF/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_5) "Building CXX object GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_cleanup.cc.o"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_cleanup.cc.o -c /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_cleanup.cc

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_cleanup.cc.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_cleanup.cc.i"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_cleanup.cc > CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_cleanup.cc.i

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_cleanup.cc.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_cleanup.cc.s"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_cleanup.cc -o CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_cleanup.cc.s

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_misc_functions.cc.o: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/flags.make
GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_misc_functions.cc.o: ../GLTF/dependencies/draco/src/draco/mesh/mesh_misc_functions.cc
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/sonnguyen/Downloads/COLLADA2GLTF/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_6) "Building CXX object GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_misc_functions.cc.o"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_misc_functions.cc.o -c /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_misc_functions.cc

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_misc_functions.cc.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_misc_functions.cc.i"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_misc_functions.cc > CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_misc_functions.cc.i

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_misc_functions.cc.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_misc_functions.cc.s"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_misc_functions.cc -o CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_misc_functions.cc.s

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_stripifier.cc.o: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/flags.make
GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_stripifier.cc.o: ../GLTF/dependencies/draco/src/draco/mesh/mesh_stripifier.cc
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/sonnguyen/Downloads/COLLADA2GLTF/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_7) "Building CXX object GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_stripifier.cc.o"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_stripifier.cc.o -c /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_stripifier.cc

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_stripifier.cc.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_stripifier.cc.i"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_stripifier.cc > CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_stripifier.cc.i

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_stripifier.cc.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_stripifier.cc.s"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/mesh_stripifier.cc -o CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_stripifier.cc.s

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/triangle_soup_mesh_builder.cc.o: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/flags.make
GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/triangle_soup_mesh_builder.cc.o: ../GLTF/dependencies/draco/src/draco/mesh/triangle_soup_mesh_builder.cc
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/sonnguyen/Downloads/COLLADA2GLTF/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_8) "Building CXX object GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/triangle_soup_mesh_builder.cc.o"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/draco_mesh.dir/src/draco/mesh/triangle_soup_mesh_builder.cc.o -c /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/triangle_soup_mesh_builder.cc

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/triangle_soup_mesh_builder.cc.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/draco_mesh.dir/src/draco/mesh/triangle_soup_mesh_builder.cc.i"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/triangle_soup_mesh_builder.cc > CMakeFiles/draco_mesh.dir/src/draco/mesh/triangle_soup_mesh_builder.cc.i

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/triangle_soup_mesh_builder.cc.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/draco_mesh.dir/src/draco/mesh/triangle_soup_mesh_builder.cc.s"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco/src/draco/mesh/triangle_soup_mesh_builder.cc -o CMakeFiles/draco_mesh.dir/src/draco/mesh/triangle_soup_mesh_builder.cc.s

draco_mesh: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/corner_table.cc.o
draco_mesh: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh.cc.o
draco_mesh: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_are_equivalent.cc.o
draco_mesh: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_attribute_corner_table.cc.o
draco_mesh: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_cleanup.cc.o
draco_mesh: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_misc_functions.cc.o
draco_mesh: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/mesh_stripifier.cc.o
draco_mesh: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/src/draco/mesh/triangle_soup_mesh_builder.cc.o
draco_mesh: GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/build.make

.PHONY : draco_mesh

# Rule to build all files generated by this target.
GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/build: draco_mesh

.PHONY : GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/build

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/clean:
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && $(CMAKE_COMMAND) -P CMakeFiles/draco_mesh.dir/cmake_clean.cmake
.PHONY : GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/clean

GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/depend:
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /Users/sonnguyen/Downloads/COLLADA2GLTF /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco /Users/sonnguyen/Downloads/COLLADA2GLTF/build /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : GLTF/dependencies/draco/CMakeFiles/draco_mesh.dir/depend

