import java.io.*;
import java.util.*;
public class FileShare {
  int total, nSum;

  FileShare(int n)
  {
    this.nSum = n;
  }

  public synchronized void access(int i)
  {
          if(i + total >= nSum)
          {
            try{
                wait();
              }
            catch (InterruptedException e) {}
          }
          total += i;
  }
  public synchronized void release(int i){
          total -= i;
          notify();
  }
  public static void main(String[] args){
  }
}
