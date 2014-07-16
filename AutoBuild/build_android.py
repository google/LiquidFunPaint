#!/usr/bin/python
# Copyright 2014 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""Android automated build script.

This script may be used for builds of LiquidFun Paint.

Optional environment variables:

ANDROID_SDK_HOME = Path to the Android SDK. Required if it is not passed on the
command line.
NDK_HOME = Path to the Android NDK. Required if it is not passed on the
command line.
MAKE_FLAGS = String to override the default make flags with for ndk-build.
ANT_PATH = Path to ant executable. Required if it is not in $PATH or passed on
the command line.
LIQUIDFUN_SRC_PATH = Path to LiquidFun source directory. Required if it is not
passed on the command line.
SWIG_BIN = Path to SWIG binary executable. Required if it is not passed on the
command line.
SWIG_LIB = Path to SWIG shared files directory. If it is not passed on the
command line, we will rely on SWIG_BIN to get it.
OUTPUT_ZIP = Path and name to the output archive of build artifacts
"""

import argparse
import distutils.spawn
import os
import shutil
import subprocess
import sys

import buildutils

_ANT_FLAGS = 'ant_flags'
_LIQUIDFUN_SRC_PATH_ENV_VAR = 'LIQUIDFUN_SRC_PATH'
_LIQUIDFUN_SRC_PATH = 'liquidfun_src_path'
_SWIG_LIB_ENV_VAR = 'SWIG_LIB'
_SWIG_LIB = 'swig_lib'
_SWIG_BIN_ENV_VAR = 'SWIG_BIN'
_SWIG_BIN = 'swig_bin'
_OUTPUT_ZIP = 'output_zip'
_OUTPUT_APK_DIR = 'output_apk_dir'


def AddArguments(parser):
  """Add module-specific command line arguments to an argparse parser.

  This will take an argument parser and add arguments appropriate for this
  module. It will also set appropriate default values.

  Args:
    parser: The argparse.ArgumentParser instance to use.
  """
  buildutils.AddArguments(parser)

  defaults = {}
  defaults[_ANT_FLAGS] = 'release'
  defaults[_LIQUIDFUN_SRC_PATH] = (os.getenv(_LIQUIDFUN_SRC_PATH_ENV_VAR) or
                                   '../../libs/liquidfun/Box2D')
  defaults[_SWIG_BIN] = (os.getenv(_SWIG_BIN_ENV_VAR) or
                         distutils.spawn.find_executable('swig'))
  defaults[_SWIG_LIB] = os.getenv(_SWIG_LIB_ENV_VAR)
  defaults[_OUTPUT_ZIP] = None
  defaults[_OUTPUT_APK_DIR] = None

  parser.add_argument('-A', '--' + _ANT_FLAGS,
                      help='Flags to use to override ant flags',
                      dest=_ANT_FLAGS, default=defaults[_ANT_FLAGS])
  parser.add_argument('-l', '--' + _LIQUIDFUN_SRC_PATH,
                      help='Path to LiquidFun/Box2D source directory',
                      dest=_LIQUIDFUN_SRC_PATH,
                      default=defaults[_LIQUIDFUN_SRC_PATH])
  parser.add_argument('--' + _SWIG_BIN,
                      help='Path to SWIG binary', dest=_SWIG_BIN,
                      default=defaults[_SWIG_BIN])
  parser.add_argument('--' + _SWIG_LIB,
                      help='Path to SWIG shared libraries', dest=_SWIG_LIB,
                      default=defaults[_SWIG_LIB])
  parser.add_argument('-z', help='Path and name to the output archive',
                      dest=_OUTPUT_ZIP, default=defaults[_OUTPUT_ZIP])
  parser.add_argument('-o', '--' + _OUTPUT_APK_DIR,
                      help='Path to copy output APKs to.',
                      dest=_OUTPUT_APK_DIR, default=defaults[_OUTPUT_APK_DIR])


class BuildEnvironment(buildutils.BuildEnvironment):

  """Class representing the build environment we will be building in.

  This class is derived from buildutils.BuildEnvironment and adds specific
  attributes for this project.
  This class resolves and exposes various build parameters as properties,
  which can be customized by users before building. It also provides methods
  to accomplish common build tasks such as executing build tools and archiving
  the resulting build artifacts.

  Attributes:
    ant_flags: Flags to pass to ant, for ant builds.
  """

  def __init__(self, arguments):
    """Constructs the BuildEnvironment with basic information needed to build.

    The build properties as set by argument parsing are also available
    to be modified by code using this object after construction.

    It is required to call this function with a valid arguments object,
    obtained either by calling argparse.ArgumentParser.parse_args() after
    adding this modules arguments via buildutils.AddArguments(), or by passing
    in an object returned from buildutils.BuildDefaults().

    Args:
      arguments: The argument object returned from ArgumentParser.parse_args().
    """
    super(BuildEnvironment, self).__init__(arguments)

    if type(arguments) is dict:
      args = arguments
    else:
      args = vars(arguments)

    self.ant_flags = args[_ANT_FLAGS]
    os.environ[_LIQUIDFUN_SRC_PATH_ENV_VAR] = args[_LIQUIDFUN_SRC_PATH]
    os.environ[_SWIG_BIN_ENV_VAR] = args[_SWIG_BIN]
    os.environ[_SWIG_LIB_ENV_VAR] = (args[_SWIG_LIB] or
                                     self.CaptureSubprocessOutput(
                                         [args[_SWIG_BIN],
                                          '-swiglib']))
    self.output_zip = args[_OUTPUT_ZIP]
    self.output_apk_dir = args[_OUTPUT_APK_DIR]

  def CaptureSubprocessOutput(self, argv):
    """Returns the output of a subprocess as run with the given argument list.

    Runs a process via popen().

    Args:
      argv: A list of process arguments starting with the binary name, in the
        form returned by shlex.

    Returns:
      The commandline output from the subprocess, with the last newline
        stripped.
    """
    try:
      if self.verbose:
        print 'Running subcommand as: %s' % str(argv)

      process = subprocess.Popen(argv, stdout=subprocess.PIPE)
      process.wait()
      return process.communicate()[0].rstrip()

    except OSError:
      return ''

  def CopyFilesWithExtension(self, dirlist, extension, output_path,
                             flatten=False, exclude=None):
    """Copy files from the specified directory path to the output path.

    Copy any files of a certain extension, present in the directories specified
    in dirlist, to the specified output path. All dirlist paths are relative
    from the project top directory.

    Args:
      dirlist: A list of directories to search for files in.
      extension: The extension of the files we are searching for.
      output_path: Path to the output directory, relative to the value of
        the project_directory property.
      flatten: If true, copy all files to the same directory without preserving
        directory structure.
      exclude: Optional list of directory names to filter from dir trees in
        dirlist.  Subdirectories with these names will be skipped when writing
        the archive.

    Raises:
      IOError: An error occurred writing or copying the archive.
    """
    outputabs = os.path.join(self.project_directory, output_path)

    for d in dirlist:
      srcdir = os.path.join(self.project_directory, d)
      for root, dirs, files in os.walk(srcdir):
        if exclude:
          for ex in exclude:
            if ex in dirs:
              dirs.remove(ex)
        for f in files:
          if f.endswith(extension):
            outabspath = outputabs
            if not flatten:
              outrelpath = os.path.relpath(root, self.project_directory)
              outabspath = os.path.join(outputabs, outrelpath)
            if not os.path.exists(outabspath):
              os.makedirs(outabspath)
            copyf = os.path.join(root, f)
            if self.verbose:
              print 'Copying %s to: %s' % (copyf, outabspath)
            shutil.copy2(copyf, outabspath)


def main():
  parser = argparse.ArgumentParser()
  AddArguments(parser)
  args = parser.parse_args()

  retval = -1

  env = BuildEnvironment(args)

  try:
    env.GitClean()
    env.BuildAndroidLibraries(['.'])
    env.RunSubprocess([env.ant_path, env.ant_flags])
    if env.output_zip is not None:
      env.MakeArchive(['bin', 'libs', 'gen', 'obj'], env.output_zip)
    if env.output_apk_dir is not None:
      env.CopyFilesWithExtension(['bin'], '.apk', env.output_apk_dir,
                                 True, ['latest'])
    retval = 0

  except buildutils.Error as e:
    print >> sys.stderr, 'Caught buildutils error: %s' % e.error_message
    retval = e.error_code

  except IOError as e:
    print >> sys.stderr, 'Caught IOError for file %s: %s' % (e.filename,
                                                             e.strerror)
    retval = -1

  return retval

if __name__ == '__main__':
  sys.exit(main())

