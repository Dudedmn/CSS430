/* P5: File System
 * CSS 430
 * Date: 6/8/2019
 * Authors: Daniel Yan and Emily Krasser
 *
 * Modified SysLib for P5 with FileSystem calls.
 */

import java.util.*;

public class SysLib {

	//formats disk, specifiy maximum number of files to be supported by disk
	public static int format(int maxFiles)
	{
		return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, Kernel.FORMAT, maxFiles, null);
	}

	//open file with given name in given mode
	public static int open(String filename, String mode)
	{
		String[] args = new String[2];
		args[0] = filename;
		args[1] = mode;
		return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE, Kernel.OPEN, 0, args);
	}

	//close file with given file descriptor
	public static int close(int fd)
	{
		return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, Kernel.CLOSE, fd, null);
	}

	//read from a given file into given buffer
	public static int read(int fd, byte[] buffer)
	{
		return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, Kernel.READ, fd, buffer);
	}

	//writes contents from given buffer into given file (file descriptor used to access file)
	public static int write(int fd, byte[] buffer)
	{
		 return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE, Kernel.WRITE, fd, buffer );
	}

	//moves seek point for given file. whence determines where to start, offset is from given whence
	public static int seek(int fd, int offset, int whence)
	{
		int[] args = new int[2];
		args[0] = offset;
		args[1] = whence;
		return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE, Kernel.SEEK, fd, args );
	}

	//removes given file from disk and file system
	public static int delete(String filename)
	{
		return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, Kernel.DELETE, 0, filename);
	}

	//returns size of given file
	public static int fsize(int fd)
	{
		return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE, Kernel.SIZE, fd, null);
	}

    public static int exec( String args[] ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.EXEC, 0, args );
    }

    public static int join( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WAIT, 0, null );
    }

    public static int boot( ) {
	return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.BOOT, 0, null );
    }

    public static int exit( ) {
	return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.EXIT, 0, null );
    }

    public static int sleep( int milliseconds ) {
	return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.SLEEP, milliseconds, null );
    }

    public static int disk( ) {
	return Kernel.interrupt( Kernel.INTERRUPT_DISK,
				 0, 0, null );
    }

    public static int cin( StringBuffer s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.READ, 0, s );
    }

    public static int cout( String s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WRITE, 1, s );
    }

    public static int cerr( String s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WRITE, 2, s );
    }

    public static int rawread( int blkNumber, byte[] b ) {
	return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.RAWREAD, blkNumber, b );
    }

    public static int rawwrite( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.RAWWRITE, blkNumber, b );
    }

    public static int sync( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.SYNC, 0, null );
    }

    public static int cread( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CREAD, blkNumber, b );
    }

    public static int cwrite( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CWRITE, blkNumber, b );
    }

    public static int flush( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CFLUSH, 0, null );
    }

    public static int csync( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CSYNC, 0, null );
    }

    public static String[] stringToArgs( String s ) {
	StringTokenizer token = new StringTokenizer( s," " );
	String[] progArgs = new String[ token.countTokens( ) ];
	for ( int i = 0; token.hasMoreTokens( ); i++ ) {
	    progArgs[i] = token.nextToken( );
	}
	return progArgs;
    }

    public static void short2bytes( short s, byte[] b, int offset ) {
	b[offset] = (byte)( s >> 8 );
	b[offset + 1] = (byte)s;
    }

    public static short bytes2short( byte[] b, int offset ) {
	short s = 0;
        s += b[offset] & 0xff;
	s <<= 8;
        s += b[offset + 1] & 0xff;
	return s;
    }

    public static void int2bytes( int i, byte[] b, int offset ) {
	b[offset] = (byte)( i >> 24 );
	b[offset + 1] = (byte)( i >> 16 );
	b[offset + 2] = (byte)( i >> 8 );
	b[offset + 3] = (byte)i;
    }

    public static int bytes2int( byte[] b, int offset ) {
	int n = ((b[offset] & 0xff) << 24) + ((b[offset+1] & 0xff) << 16) +
	        ((b[offset+2] & 0xff) << 8) + (b[offset+3] & 0xff);
	return n;
    }
}
