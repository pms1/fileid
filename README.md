# fileid

A small library that provides a unique id for a file in a filesystem that is constant even when renaming the file.

The implementation currently supports only Windows, but previous incarnations of this have had support for Unix like systems (in some lucky cases, the ids were even constant accross a Samba share). Extending the implementation to Unix is currently not planned.

