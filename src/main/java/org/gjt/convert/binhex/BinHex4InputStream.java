/*
  JBinHex
  Copyright (C) 2000, Erwin Bolwidt <ejb@klomp.org>

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

package org.gjt.convert.binhex;

import java.util.*;
import java.io.*;

/**
 This class completely decodes a BinHex4 file in three parts: the header,
 the data fork and the resource fork.
 By default, all the InputStream-derived methods work on the data fork. With
 the methods <code>useDataFork</code> and <code>useResourceFork</code> the caller
 can select from which fork this stream will read. However, because this stream
 obtains the BinHex4 file through a stream, one can only read the forks in the
 order that they are in the BinHex4 file, which is: first the data fork, then 
 the resource fork. A future version of this class that obtains it's data from
 a RandomAccessFile may be able to switch to reading from data and resource
 fork at any time.

 <p>
 Lacks something that translates between Unicode and the Mac character
 set to deal with foreign characters in filenames, types and creators.
 Anyone who has such a thing under a GPL license is invited to send it to me.

 @author Erwin Bolwidt
 */
public class BinHex4InputStream extends InputStream {
    private final static int stateBeforeHeader = 0;
    private final static int stateInDataFork = 1;
    private final static int stateInResourceFork = 2;
    private final static int stateError = 3;

    /**
     Representation of a BinHex4 header section.
     */
    public static class Header
    {
        /**
         The name that this file had before encoding in BinHex4. The bytes
		 represent characters in the Macintosh character set.
         */
        byte[]  fileName;

        /**
         The version of this file. Usually 0. This is a special feature of
         the Macintosh file system that is, to my knowledge, infrequently
		 used.
         */
        int     version;

        /**
         Four bytes containing the file type. The bytes represent
         characters in the Macintosh character set.
         */
        byte[]  type;

        /**
         Four bytes containing the creator type. The bytes represent
         characters in the Macintosh character set.
         */
        byte[]  creator;

        /**
         Macintosh file system file info flags
         */
        int     flags;
        /**
         Length of the data fork.<p>
         A java long even though it's only 32 bits in the file because I
         don't know if it's signed or unsigned in the file.
         */
        long    dataLength;
        /**
         Length of the resource fork.<p>
         A java long even though it's only 32 bits in the file because I
         don't know if it's signed or unsigned in the file.
         */
        long    resourceLength;

        /**
         Reads a header from a completely 8-bit clean BinHex4-Hqx8 stream.
         (No RLE coding allowed)
         */
        public Header(InputStream in) throws IOException
        {
            int szFileName = in.read();
            if(szFileName == -1)
                throw new EOFException("In Hqx header");
            fileName = new byte[szFileName];
            if(in.read(fileName) != szFileName)
                throw new EOFException("In Hqx header");

            version = in.read();
            if(version == -1)
                throw new EOFException("In Hqx header");

            type = new byte[4];
            if(in.read(type) != 4)
                throw new EOFException("In Hqx header");

            creator = new byte[4];
            if(in.read(creator) != 4)
                throw new EOFException("In Hqx header");

            flags = read16bits(in);

            dataLength = read32bits(in);
            resourceLength = read32bits(in);
        }

        /**
         Returns the name this file had before encoding in BinHex.
         <p>
         Since I've found no trace of a Macintosh character set converted
         to/from Unicode, this method converts using the locale-default
         character converter. This will usually only work well if your
         character set is ASCII for character codes 0-127.
         If anyone can send a converter for Mac characters to Unicode, I'll
         put that in here.
         */
        public String getFileName()
        {
            return new String(fileName);
        }

        /**
         Returns the type that this file had before encoding in BinHex.
         <p>
         Since I've found no trace of a Macintosh character set converted
         to/from Unicode, this method converts using the locale-default
         character converter. This will usually only work well if your
         character set is ASCII for character codes 0-127.
         If anyone can send a converter for Mac characters to Unicode, I'll
         put that in here.
         */
        public String getType()
        {
            return new String(type);
        }

        /**
         Returns the creator that this file had before encoding in BinHex.
         <p>
         Since I've found no trace of a Macintosh character set converted
         to/from Unicode, this method converts using the locale-default
         character converter. This will usually only work well if your
         character set is ASCII for character codes 0-127.
         If anyone can send a converter for Mac characters to Unicode, I'll
         put that in here.
         */
        public String getCreator()
        {
            return new String(creator);
        }

        /**
         Returns the version that this file had before encoding in BinHex.
         */
        public int getVersion()
        {
            return version;
        }

        /**
         Returns the file info flags that this file had before encoding in BinHex.
         */
        public int getFlags()
        {
            return flags;
        }

        /**
         Returns the length of the data fork.
         */
        public long getDataLength()
        {
            return dataLength;
        }

        /**
         Returns the length of the resource fork.
         */
        public long getResourceLength()
        {
            return resourceLength;
        }

        /**
         Returns the content of this header in a single informational String.
         */
        public String toString()
        {
            return "BinHex4InputStream.Header[\nfileName = " + new String(fileName)
                    + "\nversion = " + version + "\ntype = " + new String(type)
                    + "\ncreator = " + new String(creator) + "\nflags = " + flags
                    + "\ndataLength = " + dataLength + "\nresourceLength = " + resourceLength
                    + "\n]";
        }
    }


    /**
     Constructs a BinHex4InputStream from a stream that is a source of 7-bit
	 Hqx7 encoded data. This is the typical use for files fetched from the
     Internet or through e-mail.
     */
    public BinHex4InputStream(InputStream source)
    {
        hqxIn = new RLE_CRCInputStream(source);
    }

    /**
     Constructs a BinHex4InputStream from a stream that is either a source
	 of 7-bit Hqx7 encoded data or of pure 8-bit data in Hqx8 format. The
     flag eightBit tells this class what to expect.

     @param source
            the data source
     @param eightBit
            if true, the data source must supply 8-bit data in Hqx8 format.
            If false, the data source must supply 7-bit data in Hqx7 format.
     */
    public BinHex4InputStream(InputStream source, boolean eightBit)
    {
        hqxIn = new RLE_CRCInputStream(source, eightBit);
    }

    /**
     Returns the header section of this BinHex file in a Header object.
     */
    public Header getHeader() throws IOException
    {
        if(header == null)
            readHeader();
        return header;
    }

    private void readHeader() throws IOException
    {
        try {
            if(streamState != stateBeforeHeader)
                    throw new IOException("Wrong stream state, cannot read the header now.");
            hqxIn.resetCRC();
            header = new Header(hqxIn);
            checkDataCRC();
            switchState(stateInDataFork);
        } catch(IOException e)
		{
            switchState(stateError);
            throw e;
        }
    }

    private void switchState(int newState)
    {
        if((header == null && newState != stateError) || streamState == stateError)
            throw new IllegalStateException(
			        "Cannot switch state with header == null or in errorState");

        if(newState == stateInDataFork)
        {
            bytesLeftInFork = header.dataLength;
            hqxIn.resetCRC();
            hardEndOfFork = false;
            seenEndOfFork = false;
        }
		else if(newState == stateInResourceFork)
        {
            bytesLeftInFork = header.resourceLength;
            hqxIn.resetCRC();
            hardEndOfFork = false;
            seenEndOfFork = false;
        }
        streamState = newState;
    }

    /**
     Switch reading from the data fork. All methods derived from InputStream
     will apply to the data fork. This method cannot be called after any
     method has thrown an IOException, or after useResourceFork has been called.
     */
    public void useDataFork() throws IOException
    {
        if(streamState == stateError)
            throw new IOException("Stream is already in error state");
        else if(streamState == stateBeforeHeader)
            readHeader();
        else if(streamState == stateInResourceFork)
            throw new IOException(
			        "Sorry, no random access. Cannot switch back from "
					+ "resource to data fork.");
        else if(streamState != stateInDataFork)
            throw new IllegalStateException("Stream is in unknown state.");
    }

    /**
     Swtich to reading from the resource fork.  All methods derived from 
	 InputStream will apply to the resource fork. This method cannot be
	 called after any method has thrown an IOException.
     */
    public void useResourceFork() throws IOException
    {
        if(streamState == stateError)
            throw new IOException("Stream is already in error state");
        else if(streamState == stateBeforeHeader)
            readHeader();

        if(streamState == stateInDataFork)
        {
            skipToEndOfFork();
            switchState(stateInResourceFork);
        }

        else if(streamState != stateInResourceFork)
            throw new IllegalStateException("Stream is in unexpected state.");
    }

    public int read() throws IOException
    {
        if(streamState == stateBeforeHeader)
            useDataFork();

        if(seenEndOfFork)
        {
            // If this method is called a second time AFTER a -1, the
            // caller must know for itself and be ready to get an EOFException.
            // So set seenEOF to false again, and nextDecodedByte will throw
            // one of these.
            seenEndOfFork = false;
            return -1;
        } else if(hardEndOfFork)
            throw new EOFException("End of fork");

        if(bytesLeftInFork == 0)
        {
            hardEndOfFork = true;
            return -1;
        }

        int b = hqxIn.read();
        if(b == -1)
            throw new EOFException("Physical end-of-file before end of fork");

        bytesLeftInFork--;

        if(bytesLeftInFork == 0)
        {
            // The fork was completely read, now check the fork's CRC
            checkDataCRC();
        }
        return b;
    }

    public int read(byte[] b) throws IOException
    {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        if(seenEndOfFork)
        {
            // If this method is called a second time AFTER a -1, the
            // caller must know for itself and be ready to get an EOFException.
            // So set seenEOF to false again, and nextDecodedByte will throw
            // one of these.
            seenEndOfFork = false;
            return -1;
        } else if(hardEndOfFork)
            throw new EOFException("End of fork");

        if(bytesLeftInFork == 0)
        {
            hardEndOfFork = true;
            return -1;
        }

        if(len == 0)
            return 0;

        int toRead = (len > bytesLeftInFork) ? (int)bytesLeftInFork : len;

        int r = hqxIn.read(b, off, toRead);
        if(r <= 0)
            throw new EOFException("Physical end-of-file before end of fork");

        bytesLeftInFork -= r;

        if(bytesLeftInFork <= 0)
        {
            // The fork was completely read, now check the fork's CRC
            checkDataCRC();
        }
        return r;
    }

    private void checkDataCRC() throws IOException
    {
        int calculatedCRC = hqxIn.getCRC();
        int readCRC = read16bits(hqxIn);
        if(calculatedCRC != readCRC)
            throw new IOException("Incorrect CRC (calculated:"+calculatedCRC+" != file:"+readCRC+")");
    }

	private void skipToEndOfFork() throws IOException
    {
        skip(bytesLeftInFork);
    }

    private static long read32bits(InputStream in) throws IOException
    {
        long res = 0;
        for(int i = 0; i < 4; i++)
        {
            int v = in.read();
            if(v == -1)
            throw new EOFException("Unexpected");
            res = (res << 8) | v;
        }
        return res;
    }

    private static int read16bits(InputStream in) throws IOException
    {
        int fl1 = in.read();
        if(fl1 == -1)
            throw new EOFException("Unexpected");
        int fl2 = in.read();
        if(fl2 == -1)
            throw new EOFException("Unexpected");
        return (fl1 << 8) | fl2;
    }

    public static void main(String[] args)
    {
        try {
            BinHex4InputStream in = new BinHex4InputStream(System.in);
            System.err.println(in.getHeader());

            byte[] buf = new byte[1024];

            System.err.println("Starting to convert");
            while(true)
            {
                int r = in.read(buf);
                if(r <= 0)
                    return;
                System.out.write(buf, 0, r);
            }
        } catch(IOException e)
		{
            e.printStackTrace();
        }
    }

    /**
     One of three states: before header, in data fork, or in resource fork.
     */
    private int             streamState = stateBeforeHeader;

    /**
     How many bytes are left to read in the current fork; only valid when
     streamState != beforeHeader
     */
    private long            bytesLeftInFork;

    /**
     The header of the BinHex4 file; only available when streamState != beforeHeader
     */
    private Header          header;

    /**
     The input stream, conveniently cast to type RLE_CRCInputStream so the CRC
     related methods can be easily called.
     */
    private RLE_CRCInputStream hqxIn;

    /**
     read(byte[]) sets this if it cannot return -1 immediately. Read calls
     must return -1 and set this flag to false when they find this flag true.
     */
    private boolean         seenEndOfFork;

    /**
     Hard end of fork. If somebody reads from this stream after they've been
     returned a -1, which means seenEndOfFork == false and hardEndOfFork == true,
     then they get an EOFException.
     */
    private boolean         hardEndOfFork;
}

