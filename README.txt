RELEASE NOTES DRAFT 7:
+ ArchiveFactory Interface has changed
+ Factory can identify archives by heade
RELEASE NOTES DRAFT 6:
+ use of parameter overloading is back
+ divided between Decompressor and Comressor
- compressor does not accept filenames as string arguments
- deleted AbstractCompressor.createTempFile
+ created CompressUtils
+ Moved copy of AbstractCompressor into CompressUtils
+ Added new methods for decompressing/compressing
+ changed FileInputStream return methods to InputStream
+ refactored the archiver interface
+ Archive to Stream
+ Exceptions use initCause(Throwable) for exception chaining
+ one can add InputStreams now instead of files now

* TODO:
This list bases on comments from the user/development list which has NOT
beeing implemented yet but should be checked for implementaton or should be implemented.

** BASED ON DRAFT 6:
* handle file overwriting f.e.
	- Archiver.setOverwriteFilter(TrueFileFilter.INSTANCE)
	- Compresser.setOverwrite(true); 
* Recursivley add directories
* For both of ArchiveType and CompressorType add a valueOf(String)
  method. This is what Java 1.5 Enums have and it lets you convert a
  String, say from a config file, into an Enum. To do this right you'll
  need to keep track of other Types with a Map or something in the
  constructor. 
* TestCases are out of date
* New Feature: delete from archives
* if an archive is set with the setArchive or the getInstance(File) method
  the archives fileentrys must be added to the internal entrylist for possible manipulation
* Merge Plexus Code, if there is something which is good for compress
* Propose new name? The maingoal is not to compress, imho
* refactor and review tar, zip and bzip2 implementations
* rebuild javadoc
* check out how compress could fit in VFS best
* Check wether TAR works on solaris or not.
  "Background: The original tar format supports a maximum path size of 99
  characters. If you use Solaris tar, you will not see a problem, because
  Solaris tar extends this format beyond 99 characters but in a Solaris-only
  way. GNU tar has a different way of extending the format, so is incompatible
  with the Solaris tar. WinZip and Cygnus GNU tar 1.11.8 do not support the
  Solaris way. We recommended that you use Solaris tar to extract the archive,
  or use the jar tool or WinZip to extract the zip version."
  http://java.sun.com/products/archive/j2se/1.2.2_017/install-docs.html

** BASED ON DRAFT 5:
* ZipInputStream/TarInputStream: skimming it I don't it. It looks like it's taking
the InputStream concept and corrupting it with the notion of many
separate streams for each file in one stream. This is confusing
because it doesn't fit the expectations of an InputStream. IMO it
should be it's something similar to an Iterator that takes a raw
InputStream and provides a way to get at metadata sequentially from
the raw InputStream. From the meta data you should be able to get an
InputStream which is just for that file in the archive.

* ZipOutputStream/TarOutputStream: Same problem as TarInputStream but with an
OutputStream. Because TarOutputStream subclasses OutputStream it makes
write methods available. But using these methods are dangerous because
if you write a different number of bytes than what was passed when
putNextEntry(TarEntry) was called you corrupt the archive. A good API
doesn't let the programmer make mistakes. I'd change this to be it's
own object type that accepts TarEntrys with all the needed to add a
file in one step that either succeeds or fails.

* ZipLong, ZipShort: these are both public, and I don't see why they
need to be. They are only used inside the package with the exception
of 3 places ZipShort is used as a parameter type in a public method. I
don't see why those ZipShorts cannot be converted to shorts for the
public API and both of them made package private.
