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
include GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/depend.make

# Include the progress variables for this target.
include GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/progress.make

# Include the compile flags for this target's objects.
include GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/flags.make

GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/__/__/__/draco_enc_config.cc.o: GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/flags.make
GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/__/__/__/draco_enc_config.cc.o: draco_enc_config.cc
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/Users/sonnguyen/Downloads/COLLADA2GLTF/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Building CXX object GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/__/__/__/draco_enc_config.cc.o"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/draco_enc_config.dir/__/__/__/draco_enc_config.cc.o -c /Users/sonnguyen/Downloads/COLLADA2GLTF/build/draco_enc_config.cc

GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/__/__/__/draco_enc_config.cc.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/draco_enc_config.dir/__/__/__/draco_enc_config.cc.i"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /Users/sonnguyen/Downloads/COLLADA2GLTF/build/draco_enc_config.cc > CMakeFiles/draco_enc_config.dir/__/__/__/draco_enc_config.cc.i

GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/__/__/__/draco_enc_config.cc.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/draco_enc_config.dir/__/__/__/draco_enc_config.cc.s"
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && /usr/local/Cellar/gcc/8.1.0/bin/g++-8 $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /Users/sonnguyen/Downloads/COLLADA2GLTF/build/draco_enc_config.cc -o CMakeFiles/draco_enc_config.dir/__/__/__/draco_enc_config.cc.s

draco_enc_config: GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/__/__/__/draco_enc_config.cc.o
draco_enc_config: GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/build.make

.PHONY : draco_enc_config

# Rule to build all files generated by this target.
GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/build: draco_enc_config

.PHONY : GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/build

GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/clean:
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco && $(CMAKE_COMMAND) -P CMakeFiles/draco_enc_config.dir/cmake_clean.cmake
.PHONY : GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/clean

GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/depend:
	cd /Users/sonnguyen/Downloads/COLLADA2GLTF/build && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /Users/sonnguyen/Downloads/COLLADA2GLTF /Users/sonnguyen/Downloads/COLLADA2GLTF/GLTF/dependencies/draco /Users/sonnguyen/Downloads/COLLADA2GLTF/build /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco /Users/sonnguyen/Downloads/COLLADA2GLTF/build/GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : GLTF/dependencies/draco/CMakeFiles/draco_enc_config.dir/depend

