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
 Decodes Run-length encoding LE from a 8-bit BinHex stream and calculates
 CRCs (Cyclic Redundancy Check) from the decoded bytes as it passes them to
 the caller.

 @author Erwin Bolwidt
 */
public class RLE_CRCInputStream extends FilterInputStream {
    /**
     The size of the streambuffer. Could be anything that's not too small.
     Bigger sizes probably improve performance, up to a point.
     */
    final static int    sz_streamBuf = 1024;

    /**
     This character signals that the previous character must be repeated
     a number of times (Run-Length Encoding), unless the character after
     it is 0, in which case it simply means the literal 0x90 character.
     */
    final static int    rleChar = 0x90;

    /**
     Constructs a RLE_CRCInputStream from a stream that is a source of 7-bit
	 Hqx7 encoded data. This is the typical use for files fetched from the
     Internet or through e-mail. Internally, a Hqx7_to_Hqx8InputStream is
     created to translate from 7-bit to 8-bit format before passing the data
     through this stream.
     */
    public RLE_CRCInputStream(InputStream source)
    {
        super(new Hqx7_to_Hqx8InputStream(source));
    }

    /**
     Constructs a RLE_CRCInputStream from a stream that is either a source
	 of 7-bit Hqx7 encoded data or of pure 8-bit data in Hqx8 format. The
     flag eightBit tells this class what to expect.

     @param source
            the data source
     @param eightBit
            if true, the data source must supply 8-bit data in Hqx8 format.
            If false, the data source must supply 7-bit data in Hqx7 format.
     */
    public RLE_CRCInputStream(InputStream source, boolean eightBit)
    {
        super(eightBit ? source : new Hqx7_to_Hqx8InputStream(source));
    }


    /**
     Internal method to get the next physical byte from the superclass
     stream, while keeping buffering consistent.
     @return
        the byte that was read, or -1 if end-of-file was encountered for
        the first time. Any subsequent read throws an EOFException.
     @exception EOFException
        thrown when this method is called after it returned -1 once.
     */
    private int nextStreamByte() throws IOException
    {
        if(sbFilled == -1)
            throw new EOFException("End of file already reached.");

        if(sbIndex < sbFilled)
            // Take care to return no sign-extended numbers, hence the & 0xff
            return ((int)streamBuffer[sbIndex++]) & 0xff;

        sbFilled = super.read(streamBuffer, 0, streamBuffer.length);
        if(sbFilled <= 0)
        {
            sbFilled = -1;
            return sbFilled;
        }

        sbIndex = 0;
        // Take care to return no sign-extended numbers, hence the & 0xff
        return ((int)streamBuffer[sbIndex++]) & 0xff;
    }

	private int nextDecodedByte() throws IOException
    {
        // Check if we're still busy expanding a run-length-encoding.
        // No reads are necessary in that case.
        if(inRLE)
        {
            // Being 'inRLE' means we should repeat 'lastByte' for 'rleRepeat'
            // times, until rleRepeat is 0.
            if(--rleRepeat <= 0)
                inRLE = false;
            // lastByte remains the same
            updateCRC(lastByte);
            return lastByte;
        }

        // Read from the stream
        int b = nextStreamByte();
        if(b == -1)
        {
            // Invalidate lastByte
            lastByte = -1;
            return -1;
        }

        // Check if it's the start of a run-length-encoding.
        // RLE's need special handling.
        if(b == rleChar)
        {
            // Start of RLE
            int c = nextStreamByte();
            if(c == -1)
                throw new EOFException(
				        "Corrupted Hqx8 stream, EOF just after a "
						+ "0x90 RLE char.");
            if(c == 0)
            {
                // No RLE, just a single 0x90 character
                // lastByte must be set, because 0x90 could itself be
                // subject to RLE expansion.
                lastByte = rleChar;
                updateCRC(rleChar);
                return rleChar;
            }

            // Repeat minus two because: the first one was when it
            // the byte was returned normally, and the second time is now.
            rleRepeat = c - 2;
            if(rleRepeat > 0)
                inRLE = true;
            // lastByte remains the same
            updateCRC(lastByte);
            return lastByte;
        }
		else
        {
            // Plain old straight data passing through. Do remember this
            // as the lastByte though, because the next character could
            // be the RLE char which means that _this_ character needs
            // to be repeated.
            lastByte = b;
            updateCRC(b);
            return b;
        }
    }

    private void updateCRC(int b)
    {
        // This is from Peter Lewis' article. It's probably very inefficient
        // so if anyone can give me a better version, I'd be much obliged.
        boolean temp;
        for(int i = 0 ; i < 8; i++)
        {
            temp = (calculatedCRC & 0x8000) != 0;
            calculatedCRC = (calculatedCRC << 1) | (b >> 7);
            if(temp)
                calculatedCRC ^= 0x1021;
            b = (b << 1) & 0xff;
        }
    }

    /**
     Resets the calculated CRC to zero. If your file contains multiple sections,
     as the BinHex4 format does, you must reset it before switching to a new
     section if you want to calculate CRCs for that section.
     */
    public void resetCRC()
    {
        calculatedCRC = 0;
    }

    /**
     Returns the CRC calculated over the data that was read since the beginning
     of the stream or since the last call to resetCRC.
     Must only be called after all the data was read in a section, because
	 the CRC is updated as if two extra 0 bytes were read. This is dictated
     by the BinHex4 protocol.
     */
    public int getCRC()
    {
        updateCRC(0);
        updateCRC(0);
        return calculatedCRC & 0xffff;
    }

    public int read() throws IOException
    {
        if(seenEOF)
        {
            // If this method is called a second time AFTER a -1, the
            // caller must know for itself and be ready to get an EOFException.
            // So set seenEOF to false again, and nextDecodedByte will throw
            // one of these.
            seenEOF = false;
            return -1;
        }
        return nextDecodedByte();
    }

    public int read(byte[] b) throws IOException
    {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        if(seenEOF)
        {
            // If this method is called a second time AFTER a -1, the
            // caller must know for itself and be ready to get an EOFException.
            // So set seenEOF to false again, and nextDecodedByte will throw
            // one of these.
            seenEOF = false;
            return -1;
        }

        for(int i = off, max = off+len; i < max; i++)
        {
            int t = nextDecodedByte();
            if(t == -1) {
                if(i == off)
                    // No data read yet, so safe to return -1
                    return -1;
                seenEOF = true;
                return i - off;
            }
            b[i] = (byte)t;
        }
        return len;
    }


    public static void main(String[] args)
    {
        try {
            InputStream in = new RLE_CRCInputStream(System.in);
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
     True if private method nextDecodedByte is still repeating a character
     that was part of a Run-Length Encoding sequence.
     */
    private boolean inRLE;

    /**
     The last byte that was returned by nextDecodedByte. When nextDecodedByte
     encounters a RLE-repeat code, it repeats this character a number of times.
     */
    private int     lastByte;

    /**
     If inRLE is true, then nextDecodedByte will return the byte
	 <code>lastByte</code> another <code>rleRepeat</code> times.
     */
    private int     rleRepeat;

    /**
     Bytes that were read from the stream but not all yet processed.
     */
    private byte[]  streamBuffer = new byte[sz_streamBuf];

    /**
     The next byte in the stream buffer to process.
     */
    private int     sbIndex = 0;

    /**
     How many bytes in the buffer are stream data; the buffer could
     be longer than that.
     */
    private int     sbFilled = 0;

    /**
     The read(byte[], int, int) call cannot immediately signal eof with a -1
     when it sees it, since it may already have read some data. So it sets
     this flag, which it checks the next time it is called.
     */
    private boolean seenEOF = false;

    /**
     The CRC value that is being calculated.
     */
    private int     calculatedCRC;
}
