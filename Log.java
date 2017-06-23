import java.io.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/* class for saving the server to the file. If you want to
  save more stuff about the server to file...you'll have to add
  that value in the second constructor. Then overwrite the 
  log variable of the server. After that you must call log.save()
  to write to file. 
  */


public class Log implements java.io.Serializable{
  //add variables to save here.  

  // public Server logServer;
  public Integer serverId;
  public ArrayList<Integer> activeIds;

  Log() {
    //default constructor
    serverId = -1;
    activeIds = new ArrayList<Integer>();
  }

  Log(int _serverId, ArrayList<Integer> _activeIds) {
    //Constructor for building logs
    serverId = _serverId;
    activeIds = _activeIds;
  }

  public synchronized void save() {
    //This method saves the server object in an ser file in logs folder.
    try {
      FileOutputStream fout = new FileOutputStream("logs/log_" + Integer.toString(this.serverId) + ".ser");
      ObjectOutputStream out  = new ObjectOutputStream(fout);
      out.writeObject(this);
      out.close();
      fout.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public void load(Integer pid) {
    System.out.println(pid);
    try {
      FileInputStream fin = new FileInputStream("logs/log_" + Integer.toString(pid)+ ".ser");
      ObjectInputStream in = new ObjectInputStream(fin);
      Log e = (Log)in.readObject();

      //add variables to load here. 
      serverId = e.serverId;
      activeIds = e.activeIds;

      in.close();
      fin.close();
    }catch(Exception i) {
      System.out.println("Exception while loading. Check Log class.");
      i.printStackTrace();
      return;
    } 
  }
}
