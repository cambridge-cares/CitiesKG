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
include dependencies/ahoy/CMakeFiles/ahoy.dir/depend.make

# Include the progress variables for this target.
include dependencies/ahoy/CMakeFiles/ahoy.dir/progress.make

# Include the compile flags for this target's objects.
include dependencies/ahoy/CMakeFiles/ahoy.dir/flags.make

dependencies/ahoy/CMakeFiles/ahoy.dir/src/Parser.cpp.o: dependencies/ahoy/CMakeFiles/ahoy.dir/flags.make
dependencies/ahoy/CMakeFiles/ahoy.dir/src/Parser.cpp.o: ../dependencies/ahoy/src/Parser.cpp
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/sonnguyen/Downloads/COLLADA2GLTF/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Building CXX object dependencies/ahoy/CMakeFiles/ahoy.dir/src/Parser.cpp.o"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/dependencies/ahoy && /usr/local/Cellar/gcc/8.1.0/bin/g++-8  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/ahoy.dir/src/Parser.cpp.o -c /Users/sonnguyen/Downloads/COLLADA2GLTF/dependencies/ahoy/src/Parser.cpp

dependencies/ahoy/CMakeFiles/ahoy.dir/src/Parser.cpp.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/ahoy.dir/src/Parser.cpp.i"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/dependencies/ahoy && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/sonnguyen/Downloads/COLLADA2GLTF/dependencies/ahoy/src/Parser.cpp > CMakeFiles/ahoy.dir/src/Parser.cpp.i

dependencies/ahoy/CMakeFiles/ahoy.dir/src/Parser.cpp.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/ahoy.dir/src/Parser.cpp.s"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/dependencies/ahoy && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/sonnguyen/Downloads/COLLADA2GLTF/dependencies/ahoy/src/Parser.cpp -o CMakeFiles/ahoy.dir/src/Parser.cpp.s

dependencies/ahoy/CMakeFiles/ahoy.dir/src/TypedOption.cpp.o: dependencies/ahoy/CMakeFiles/ahoy.dir/flags.make
dependencies/ahoy/CMakeFiles/ahoy.dir/src/TypedOption.cpp.o: ../dependencies/ahoy/src/TypedOption.cpp
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/sonnguyen/Downloads/COLLADA2GLTF/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_2) "Building CXX object dependencies/ahoy/CMakeFiles/ahoy.dir/src/TypedOption.cpp.o"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/dependencies/ahoy && /usr/local/Cellar/gcc/8.1.0/bin/g++-8  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/ahoy.dir/src/TypedOption.cpp.o -c /Users/sonnguyen/Downloads/COLLADA2GLTF/dependencies/ahoy/src/TypedOption.cpp

dependencies/ahoy/CMakeFiles/ahoy.dir/src/TypedOption.cpp.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/ahoy.dir/src/TypedOption.cpp.i"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/dependencies/ahoy && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/sonnguyen/Downloads/COLLADA2GLTF/dependencies/ahoy/src/TypedOption.cpp > CMakeFiles/ahoy.dir/src/TypedOption.cpp.i

dependencies/ahoy/CMakeFiles/ahoy.dir/src/TypedOption.cpp.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/ahoy.dir/src/TypedOption.cpp.s"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/dependencies/ahoy && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/sonnguyen/Downloads/COLLADA2GLTF/dependencies/ahoy/src/TypedOption.cpp -o CMakeFiles/ahoy.dir/src/TypedOption.cpp.s

# Object files for target ahoy
ahoy_OBJECTS = \
"CMakeFiles/ahoy.dir/src/Parser.cpp.o" \
"CMakeFiles/ahoy.dir/src/TypedOption.cpp.o"

# External object files for target ahoy
ahoy_EXTERNAL_OBJECTS =

dependencies/ahoy/libahoy.a: dependencies/ahoy/CMakeFiles/ahoy.dir/src/Parser.cpp.o
dependencies/ahoy/libahoy.a: dependencies/ahoy/CMakeFiles/ahoy.dir/src/TypedOption.cpp.o
dependencies/ahoy/libahoy.a: dependencies/ahoy/CMakeFiles/ahoy.dir/build.make
dependencies/ahoy/libahoy.a: dependencies/ahoy/CMakeFiles/ahoy.dir/link.txt
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --bold --progress-dir=/Users/sonnguyen/Downloads/COLLADA2GLTF/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_3) "Linking CXX static library libahoy.a"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/dependencies/ahoy && $(CMAKE_COMMAND) -P CMakeFiles/ahoy.dir/cmake_clean_target.cmake
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/dependencies/ahoy && $(CMAKE_COMMAND) -E cmake_link_script CMakeFiles/ahoy.dir/link.txt --verbose=$(VERBOSE)

# Rule to build all files generated by this target.
dependencies/ahoy/CMakeFiles/ahoy.dir/build: dependencies/ahoy/libahoy.a

.PHONY : dependencies/ahoy/CMakeFiles/ahoy.dir/build

dependencies/ahoy/CMakeFiles/ahoy.dir/clean:
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/dependencies/ahoy && $(CMAKE_COMMAND) -P CMakeFiles/ahoy.dir/cmake_clean.cmake
.PHONY : dependencies/ahoy/CMakeFiles/ahoy.dir/clean

dependencies/ahoy/CMakeFiles/ahoy.dir/depend:
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /Users/sonnguyen/Downloads/COLLADA2GLTF /Users/sonnguyen/Downloads/COLLADA2GLTF/dependencies/ahoy /Users/sonnguyen/Downloads/COLLADA2GLTF/build /Users/sonnguyen/Downloads/COLLADA2GLTF/build/dependencies/ahoy /Users/sonnguyen/Downloads/COLLADA2GLTF/build/dependencies/ahoy/CMakeFiles/ahoy.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : dependencies/ahoy/CMakeFiles/ahoy.dir/depend

