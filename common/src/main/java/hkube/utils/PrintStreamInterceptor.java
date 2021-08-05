package hkube.utils;

import java.io.OutputStream;
import java.io.PrintStream;

public class PrintStreamInterceptor extends PrintStream {
    IPrinter printer;
    public PrintStreamInterceptor(OutputStream out, IPrinter printer)
    {
        super(out, true);
        this.printer = printer;
    }
    @Override
    public void print(String s)
    {   printer.print(s);
        super.print(s);
    }
    @Override
    public void println(String s)
    {   printer.print(s);
        super.println(s);
    }
    @Override
    public void print(int s)
    {   printer.print(s+"");
        super.print(s);
    }
    @Override
    public void println(int s)
    {   printer.print(s+"");
        super.println(s);
    }
    @Override
    public void print(boolean s)
    {   printer.print(s+"");
        super.print(s);
    }
    @Override
    public void println(boolean s)
    {   printer.print(s+"");
        super.println(s);
    }
}

