[![](https://jitpack.io/v/umjammer/jbinhex.svg)](https://jitpack.io/#umjammer/jbinhex)

# JBinHex

JBinHex is both a library and a command-line tool to decode files in
the Apple Macintosh BinHex 4.0 format.

## Current version: 0.5

## Version history

  * 0.5 First released version

## Limitations in this version

  * This version does not support segmented files such as used
  on `comp.binaries.mac.*` newsgroups
  * Documentation is limited
  * Command line tool has does not check wether the command line
  parameters are completely correct

## Possible future features

  * Encoding of BinHex files
  * File-based interface that allows to switch between data and
  resource fork at any moment, instead of the predetermined order
  that the stream-based interface dictates

## Command-line tool

The class name of the command-line tool is `org.gjt.convert.binhex.DeBinHex`

It accepts the following command line parameters:

  * Either `-u &lt;url&gt;` or `-f &lt;file&gt;`
  to specify the source BinHexed file. If neither of those options
  is present, `DeBinHex` reads `stdin`.
  * `-d` to decode the data fork. It will be put in
  the file with the name that came from the BinHex header.
  * `-df &lt;filename&gt;` to decode the data fork
  to the named file instead of the name that came from the BinHex
  header.
  * `-r` to decode the resource fork. It will be put
  in the file with the name that came from the BinHex header, with
  the extension &quot;`.resource`&quot; appended to
  it.
  * `-rf &lt;filename&gt;` to decode the resource
  fork to the named file instead of the name that came from the
  BinHex header.
  * Both `-d`/`-df` options and `-r`/`-rf`
  may be present at the same time. If none of these options is
  present, `DeBinHex` will decode the data fork as if
  the `-d` options was specified.
  * `-h` to only show the header of the BinHex file
  on `stdout`. The decoding options are ignored.

## Javadoc

The [Javadoc of the classes](https://www.klomp.org/JBinHex/javadoc/index.html) is included in the 
distribution and available online.

## Download

[Download the complete package](http://www.klomp.org/packages/JBinHex.tar.gz) including source, javadoc and jarfile with classes (36 Kb).

## License

The package is licensed under the **GNU General Public License**,
also known as the GPL license. See file [COPYING](https://www.klomp.org/JBinHex/COPYING) for 
details.

## References

  * Article written by Yves Lempereur, available as Appendix A in [rfc1741](http://www.rfc.net/get2.php3/rfc1741.html)</A>
  * Article titled [BinHex 4.0 Definition](http://wuarchive.wustl.edu/systems/mac/umich.edu/misc/documentation/binhex4.0specs.txt) by Peter N Lewis

## Copyright

All files in the package and on this site Copyright 2000 by Erwin
Bolwidt, &lt;[ejb@klomp.org](mailto:ejb@klomp.org)&gt;

----
This page was last updated at april 8, 2000.
