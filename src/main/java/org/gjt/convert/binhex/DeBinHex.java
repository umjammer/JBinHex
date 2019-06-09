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
import java.net.*;

/**
 Command line program to decode binhex files from the harddisk or from
 the web.

 It accepts the following command line parameters:</P>

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

 @author Erwin Bolwidt
 */
public class DeBinHex
{

    public static void main(String[] args) throws Exception
    {
        String inFile = findValueOption("-f", args);
        InputStream binhexIn = System.in;
        if(inFile != null)
            binhexIn = new FileInputStream(inFile);
        else 
		{
            String urlString = findValueOption("-u", args);
            if(urlString != null)
            {
                URL url = new URL(urlString);
                binhexIn = url.openConnection().getInputStream();
            }
        }

        if(findOption("-h", args))
        {
            action(binhexIn, true, false, null, false, null);
            return;
        }
        String dataFile = null;
        String resourceFile = null;
        boolean doData = false;
        boolean doResource = false;
        if((dataFile = findValueOption("-df", args)) != null)
            doData = true;
        if(findOption("-d", args))
            doData = true;
        if((resourceFile = findValueOption("-rf", args)) != null)
            doResource = true;
        if(findOption("-r", args))
            doResource = true;
        if(!doResource && !doData)
            // The user didn't specify anything to do, so let's do the data
            // fork.
            doData = true;
        action(binhexIn, false, doData, dataFile, doResource, resourceFile);
    }

    private static String findValueOption(String name, String[] args)
    {
        for(int i = 0; i < args.length; i++)
        {
            if(args[i].equals(name))
            {
                if(i + 1 < args.length)
                    return args[i+1];
                else
                    throw new RuntimeException(
					        "Cannot use option that needs an argument "
							+ "as the last option");
            }
        }
        return null;
    }

    private static boolean findOption(String name, String[] args)
    {
        for(int i = 0; i < args.length; i++)
        {
            if(args[i].equals(name))
                return true;
        }
        return false;
    }

    public static void action(
	        InputStream binhexIn, boolean justHeader,
	        boolean doData, String dataOut,
			boolean doResource, String resourceOut) throws IOException
    {
        BinHex4InputStream binhex;

        binhex = new BinHex4InputStream(binhexIn);
        if(justHeader)
        {
            System.out.println(binhex.getHeader());
            return;
        }

        String fileName = binhex.getHeader().getFileName();

        if(doData && dataOut == null)
            dataOut = fileName;

        if(doResource && resourceOut == null)
            resourceOut = fileName.concat(".resource");

        if(doData)
        {
            FileOutputStream out = new FileOutputStream(dataOut);
            try {
                pump(binhex, out);
            } finally {
                out.close();
            }
        }

        if(doResource)
        {
            FileOutputStream out = new FileOutputStream(resourceOut);
            binhex.useResourceFork();
            try {
                pump(binhex, out);
            } finally {
                out.close();
            }
        }
    }

    private static void pump(InputStream in, OutputStream out) throws IOException
    {
        byte[] buf = new byte[1024];
        while(true)
        {
            int r = in.read(buf);
            if(r <= 0)
                return;
            out.write(buf, 0, r);
        }
    }

}
