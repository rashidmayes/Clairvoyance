/*******************************************************************************
 * Copyright (c) 2009 Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Contributors:
 * 
 * Astrient Foundation Inc. 
 * www.astrientfoundation.org
 * rashid@astrientfoundation.org
 * Rashid Mayes 2009
 *******************************************************************************/
package com.rashidmayes.clairvoyance.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.Locale;


public final class FileUtil
{
    public static final double KIB = new Double(Math.pow(2,10)).intValue();
    public static final double MIB = new Double(Math.pow(2,20)).intValue();
    public static final double GIB = new Double(Math.pow(2,30)).intValue();
    
    public static String getSizeString(long number, Locale locale)
    {
        String suffix;
        double sz;   
        
        if ( number == 0 ) {
        	return "0";
        } else if ( number >= GIB ) {
            //gb
            sz = ( number / GIB );
            suffix = "GB";
        }
        else if ( number >= MIB )
        {
            //mb
            sz = ( number / MIB );
            suffix = "MB";
        }
        else if ( number >= KIB )
        {
            //kb
            sz = number / KIB;
            suffix = "KB";
        }
        else
        {
            sz = number;
            suffix = "B";
        }
        
        
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        nf.setMaximumFractionDigits(2);
        
        return nf.format(sz) + suffix;
    }
    
    
    public static boolean copyFile(File source, File destination,long chunkSize, boolean overwrite) throws IOException
    {
        if ( destination.exists() && !overwrite )
        {
            return false;
        }
        else if ( destination.exists() && !destination.canWrite() )
        {
            return false;
        }
        else
        {
            FileChannel sourceChannel = null;
            FileChannel destinationChannel = null;
            FileInputStream fis = null;
            FileOutputStream fos = null;
            try
            {
            	fis = new FileInputStream(source);
            	fos = new FileOutputStream(destination);
            	
                sourceChannel = fis.getChannel();
                destinationChannel = fos.getChannel();
                
                if ( chunkSize == 0 )
                {
                    chunkSize = sourceChannel.size();
                }
                
                long transferred = 0;
                long stop = sourceChannel.size();
                
                for (long maxReps = ((stop / chunkSize)+1)*2, reps = 0; (reps < maxReps) && (transferred < stop); reps++)
                {
                    transferred += sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
                }

                return ( transferred >= stop );
            }
            catch (IOException e)
            {
                throw e;
            }
            finally
            {
                try { if (sourceChannel != null) sourceChannel.close(); } catch (Exception e) {}
                try { if (destinationChannel != null) destinationChannel.close(); } catch (Exception e) {}
                try { if (fis != null) fis.close(); } catch (Exception e) {}
                try { if (fos != null) fos.close(); } catch (Exception e) {}

            }
        }
    }
    
    public static String prettyFileName(String fileName, String ext, boolean spaces)
	{
		String pattern = ( spaces ) ? "[^a-zA-Z0-9\\.\\-\\_\\ ]+" : "[^a-zA-Z0-9\\.\\-\\_]+";
		String name = fileName.replaceAll(pattern,"_").trim();
		if ( ext != null )
		{
			name = name  + "." + ext;
		}
		
		return name;
	} 
}
