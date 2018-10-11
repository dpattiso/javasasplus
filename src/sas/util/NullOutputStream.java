package sas.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * Equivalent to /dev/null output stream
 * @author David Pattison
 *
 */
public class NullOutputStream extends OutputStream
{

	@Override
	public void write(int b) throws IOException
	{
	}
}
