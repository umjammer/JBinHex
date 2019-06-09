<HTML>
<HEAD>
  <TITLE>JBinHex</TITLE>
</HEAD>
<BODY BGCOLOR="#ffffff" LINK="#ff0000" ALINK="#ff00ff" VLINK="#990000">

<H1 ALIGN="center">JBinHex</H1>

<P><FONT SIZE=+1>
JBinHex is both a library and a command-line tool to decode files in
the Apple Macintosh BinHex 4.0 format.
</FONT></P>

<H2>Current version: 0.5</H2>

<H3>Version history:</H3>

<DL>
  <DT>0.5
  <DD>First released version
</DL>

<H3>Limitations in this version:</H3>

<MENU>
  <LI>This version does not support segmented files such as used
  on comp.binaries.mac.* newsgroups
  <LI>Documentation is limited
  <LI>Command line tool has does not check wether the command line
  parameters are completely correct
</MENU>

<H3>Possible future features:</H3>

<UL>
  <LI>Encoding of BinHex files
  <LI>File-based interface that allows to switch between data and
  resource fork at any moment, instead of the predetermined order
  that the stream-based interface dictates
</UL>

<H3>Command-line tool</H3>

<P>The class name of the command-line tool is <CODE>org.gjt.convert.binhex.DeBinHex</CODE></P>

<P>It accepts the following command line parameters:</P>

<MENU>
  <LI>Either <CODE>-u &lt;url&gt;</CODE> or <CODE>-f &lt;file&gt;</CODE>
  to specify the source BinHexed file. If neither of those options
  is present, <CODE>DeBinHex</CODE> reads <CODE>stdin</CODE>.
  <LI><CODE>-d</CODE> to decode the data fork. It will be put in
  the file with the name that came from the BinHex header.
  <LI><CODE>-df &lt;filename&gt;</CODE> to decode the data fork
  to the named file instead of the name that came from the BinHex
  header.
  <LI><CODE>-r</CODE> to decode the resource fork. It will be put
  in the file with the name that came from the BinHex header, with
  the extension &quot;<CODE>.resource</CODE>&quot; appended to
  it.
  <LI><CODE>-rf &lt;filename&gt;</CODE> to decode the resource
  fork to the named file instead of the name that came from the
  BinHex header.
  <LI>Both <CODE>-d</CODE>/<CODE>-df</CODE> options and <CODE>-r</CODE>/<CODE>-rf</CODE>
  may be present at the same time. If none of these options is
  present, <CODE>DeBinHex</CODE> will decode the data fork as if
  the <CODE>-d</CODE> options was specified.
  <LI><CODE>-h</CODE> to only show the header of the BinHex file
  on <CODE>stdout</CODE>. The decoding options are ignored.
</MENU>

<H3>Javadoc</H3>
The <A HREF="javadoc/index.html">Javadoc of the classes</A> is included in the 
distribution and available online.

<H3>Download</H3>

<P><A HREF="http://www.klomp.org/packages/JBinHex.tar.gz">Download the complete package</A> including source, javadoc and jarfile with classes (36 Kb).</P>

<H3>License</H3>

<P>The package is licensed under the <B>GNU General Public License</B>,
also known as the GPL license. See file <A HREF="COPYING">COPYING</A> for 
details.</P>

<H3>References</H3>

<UL>
  <LI>Article written by Yves Lempereur, available as Appendix A in <A HREF="http://www.rfc.net/get2.php3/rfc1741.html">rfc1741</A>
  <LI>Article titled <A HREF="http://wuarchive.wustl.edu/systems/mac/umich.edu/misc/documentation/binhex4.0specs.txt">&quot;BinHex 4.0 Definition&quot;</A> by Peter N Lewis
</UL>

<H3>Copyright</H3>

<P>All files in the package and on this site Copyright 2000 by Erwin
Bolwidt, &lt;<A HREF="mailto:ejb@klomp.org">ejb@klomp.org</A>&gt;</P>

<HR ALIGN=LEFT>
<FONT SIZE="-1">This page was last updated at april 8, 2000.</FONT>


</BODY>
</HTML>
