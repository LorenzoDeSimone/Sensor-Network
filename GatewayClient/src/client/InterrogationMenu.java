//Lorenzo De Simone N880090

package client;

import ClientInfo.ClientInfo;
import ClientInfo.ClientMessage;
import static client.Client.centerWindow;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import nodenetwork.NodeInfo;
import org.glassfish.jersey.client.JerseyWebTarget;
import sensors.Measurement;

public class InterrogationMenu extends javax.swing.JFrame
{
    private static final int ID_COLUMN=0, 
                             TYPE_COLUMN=1, 
                             START_TIME_COLUMN=2,
                             STATUS_COLUMN=3;
    
    private final ClientInfo Info;
    private final JerseyWebTarget Service;
    private final HashMap <String,Integer> IDRow;//Gets Row number from Sensor ID in costant time
    private final ServerSocket InputSocket;
    
    public InterrogationMenu(ClientInfo Info, JerseyWebTarget Service, ServerSocket InputSocket, String Username)
    {
      this.Info=Info;
      this.Service=Service;
      this.InputSocket=InputSocket;
      
      IDRow= new HashMap<String,Integer>();
      
      initComponents();
      
      centerWindow(this); 
      setResizable(false);
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      setTitle("User: "+Username);

      QueryResultsJTextArea.setEditable(false);
      addWindowListener(new WindowAdapter()
      {
        @Override
        public void windowClosing(WindowEvent evt)
        {logout();}
      });

      SensorThread SensorUpdater= new SensorThread();
      SensorJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      SensorJTable.setDefaultEditor(Object.class, null);

      initSensorJTable(); 
      SensorUpdater.start();
      setVisible(true);
    }
    
    private void initSensorJTable()
    {
      //Asking the gateway for active nodes: these ones will be the first shown
      HashSet<NodeInfo> ActiveNetwork= getNetwork();
      int i=0;
      synchronized(SensorJTable)
      {
        for(NodeInfo Node : ActiveNetwork)
        {
          DefaultTableModel model = (DefaultTableModel) SensorJTable.getModel();
          model.addRow(new Object[]{Node.getID(), Node.getSensorType().name(), getReadableTime(Node.getStartTime()),"Active"});
          IDRow.put(Node.getID(),i++);
        }
      
        //Asking the gateway for all nodes
        HashSet<NodeInfo> AllSensors = getAllNodes();
        //Removing all nodes that are active: Now the Set contains only inactive nodes
        AllSensors.removeAll(ActiveNetwork);
      
        for(NodeInfo Node : AllSensors)
        {
          DefaultTableModel model = (DefaultTableModel) SensorJTable.getModel();
          model.addRow(new Object[]{Node.getID(), Node.getSensorType().name(), getReadableTime(Node.getStartTime()),"Inactive"});
          IDRow.put(Node.getID(),i++);
        }
      }
    }
    
  private void logout()
  {     
    Gson gson = new Gson();
    String JsonInfo= gson.toJson(Info);
    
    Response answer= Service.path("GatewayServices").path("Client").path("logout")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.entity(JsonInfo,MediaType.APPLICATION_JSON));
    
    if(answer.getStatus()==Response.Status.ACCEPTED.getStatusCode())
    {
      JOptionPane.showMessageDialog(null, "Logout Successful");
    }
    else if(answer.getStatus()==Response.Status.NOT_FOUND.getStatusCode())
      JOptionPane.showMessageDialog(null, "Username not registered.\n"
                                    + "Probably the gateway disconnected after client registration");
    else
      JOptionPane.showMessageDialog(null, "Unknown error:\n"+answer);
    try
    {InputSocket.close();}
    catch (IOException ex)
    {Logger.getLogger(InterrogationMenu.class.getName()).log(Level.SEVERE, null, ex);}
    this.dispose();
  }
  
  private boolean isOperationNameCorrect(String OperationName)
  {
    return (OperationName.equals("getSingleAverage")||
            OperationName.equals("getSingleMaximum")||
            OperationName.equals("getSingleMinimum")||
            OperationName.equals("getSingleLast")   ||
            OperationName.equals("getTotalAverage") ||
            OperationName.equals("getTotalMaximum") ||
            OperationName.equals("getTotalMinimum")
           );
  }
  
  private void executeGatewayQuery(String OperationName)
  {  
    long t1=-1,t2=-1;
    int rowIndex=-1;
    JerseyWebTarget Target=Service.path("GatewayServices").path("Network")
                                  .path(OperationName);
    String Type=null;
    String ID=null;

    if(!isOperationNameCorrect(OperationName))
      return;
    
    synchronized(SensorJTable)
    {
      //All operations that require a single sensor selected from the table    
      if(OperationName.equals("getSingleAverage")||
         OperationName.equals("getSingleMaximum")||
         OperationName.equals("getSingleMinimum")||
         OperationName.equals("getSingleLast"))
      {
        rowIndex=SensorJTable.getSelectedRow();
        if(rowIndex==-1)
        {
          JOptionPane.showMessageDialog(null,"Please select a sensor from the table");
          return;
        }
        else
        {
          ID=SensorJTable.getValueAt(rowIndex, ID_COLUMN).toString();
          Type=SensorJTable.getValueAt(rowIndex, TYPE_COLUMN).toString();
        }
      }
    }
    //All operations that require T1 and T2 Field filled
    if(OperationName.equals("getSingleAverage")||
       OperationName.equals("getSingleMaximum")||
       OperationName.equals("getSingleMinimum")||
       OperationName.equals("getTotalAverage") ||
       OperationName.equals("getTotalMaximum") ||
       OperationName.equals("getTotalMinimum"))
    {
      try
      {
        t1=getMillisecfromTimeString(T1JTextField.getText());
        t2=getMillisecfromTimeString(T2JTextField.getText());
      }
      catch(NumberFormatException e)
      {
        JOptionPane.showMessageDialog(null,"Please insert Time in correct format");
        return;
      }
      if(t1>t2)
      {
        JOptionPane.showMessageDialog(null,"T1 must be lower than T2");
        return;
      }
    }
    
    //Single Node Operations
    if(ID!=null)
    {
      Response answer= Service.path("GatewayServices").path("Network")
                              .path(OperationName).path(ID).path(Type).path(""+t1).path(""+t2)
                              .request(MediaType.APPLICATION_JSON).get();
      if(answer.getStatus()==Response.Status.ACCEPTED.getStatusCode())
      {
        String RequestedValue=answer.readEntity(String.class);
        QueryResultsJTextArea.setText("Requested measurement for ["+ ID +"] is: "+RequestedValue+" in the range\n("+
                                       T1JTextField.getText()+") -> ("+T2JTextField.getText()+")");
      }
      else
        QueryResultsJTextArea.setText("No measurement for ["+ ID +"] in the range\n("+
                                       T1JTextField.getText()+") -> ("+T2JTextField.getText()+")");
    }
    //Multiple Node Operations
    else
    {
      if(AccelerometerCheckBox.isSelected())
        Type="accelerometer";
      else if(LightCheckBox.isSelected())
        Type="light";
      else if(TemperatureCheckBox.isSelected())
        Type="temperature";
      else
      {
        JOptionPane.showMessageDialog(null,"Please check one of the type checkboxes");
        return; 
      }
      
      Response answer= Service.path("GatewayServices").path("Network")
                              .path(OperationName).path(Type).path(""+t1).path(""+t2)
                              .request(MediaType.APPLICATION_JSON).get();
      if(answer.getStatus()==Response.Status.ACCEPTED.getStatusCode())
      {
        String RequestedValue=answer.readEntity(String.class);
        QueryResultsJTextArea.setText("Requested measurement for "+Type+ " sensors is: "+RequestedValue+" in the range\n("+
                                       T1JTextField.getText()+") -> ("+T2JTextField.getText()+")");
      }
      else
        QueryResultsJTextArea.setText("No measurement for "+Type+ " sensors in the range\n("+
                                       T1JTextField.getText()+") -> ("+T2JTextField.getText()+")");
    }
  }
   
  private void getLastMeasurement()
  {
    int rowIndex;
    synchronized(SensorJTable)
    {rowIndex=SensorJTable.getSelectedRow();}
        if(rowIndex==-1)
    {
      JOptionPane.showMessageDialog(null,"Please select a sensor from the table");
      return;
    }
    
    String ID,Type;
    synchronized(SensorJTable)
    {
      ID=SensorJTable.getValueAt(rowIndex, ID_COLUMN).toString();
      Type=SensorJTable.getValueAt(rowIndex, TYPE_COLUMN).toString();    
    }
    
    Response answer= Service.path("GatewayServices").path("Network")
                            .path("getLastMeasurement").path(ID).path(Type)
                            .request(MediaType.APPLICATION_JSON).get();
          
    if(answer.getStatus()==Response.Status.ACCEPTED.getStatusCode())
    {
      String JsonMeasurement=answer.readEntity(String.class);
      Gson gson= new Gson();
      Measurement LastMeasurement=gson.fromJson(JsonMeasurement, Measurement.class);
      QueryResultsJTextArea.setText("Last measurement for sensor "+ ID +" is:"
                                    + " "+LastMeasurement.getValue() 
                                    +"\nTIMESTAMP: "+ getReadableTime(LastMeasurement.getTimestamp()));
    }
    else if(answer.getStatus()==Response.Status.CONFLICT.getStatusCode())
      QueryResultsJTextArea.setText("No measurement for this node\n");
    else
      JOptionPane.showMessageDialog(null, "Unknown error:\n"+answer);
  }
  
  public static long getMillisecfromTimeString(String Time) throws NumberFormatException
  {
    //Hours Parsing
    int index = Time.indexOf(':');
    if(index==-1)
      throw new NumberFormatException();
    String Hours= Time.substring(0, index);
    if(Hours ==null || Hours.length()>2 || Hours.length()<1)
      throw new NumberFormatException();
    int intHours= Integer.parseInt(Hours);
    if(intHours<0 || intHours>23)
      throw new NumberFormatException();
    long millisecHours= TimeUnit.HOURS.toMillis(intHours);
        
    //Minutes Parsing
    int newIndex= Time.indexOf(':',index+1);
    if(newIndex==-1)
      throw new NumberFormatException();
    String Minutes= Time.substring(index+1, newIndex);
    if(Minutes ==null || Minutes.length()>2 || Minutes.length()<1)
      throw new NumberFormatException(); 
    int intMinutes=Integer.parseInt(Minutes);
    if(intMinutes<0 || intMinutes>59)
      throw new NumberFormatException();
    long millisecMinutes= TimeUnit.MINUTES.toMillis(intMinutes);
    
    //Seconds Parsing
    String Seconds= Time.substring(newIndex+1, Time.length());
    if(Seconds ==null || Seconds.length()>2 || Seconds.length()<1)
      throw new NumberFormatException(); 
    int intSeconds= Integer.parseInt(Seconds);
    if(intSeconds<0 || intSeconds>59)
      throw new NumberFormatException();
    long millisecSeconds= TimeUnit.SECONDS.toMillis(intSeconds);

    return millisecHours+millisecMinutes+millisecSeconds;
  }
 
  private boolean isJTextFieldEmpty(JTextField TextField)
  {
    return TextField.getText().equals("");
  }
  
  private HashSet<NodeInfo> getAllNodes()
  {
    HashSet<NodeInfo> ConvertedNetwork =null;
    Response answer= Service.path("GatewayServices").path("Network").path("getAllNodes")
    .request(MediaType.APPLICATION_JSON)
    .get();
    if(answer.getStatus()==Response.Status.ACCEPTED.getStatusCode())
    {
      Gson gson= new Gson();
      String JsonNetwork= answer.readEntity(String.class);      
      java.lang.reflect.Type type = new TypeToken<HashSet<NodeInfo>>(){}.getType();
      ConvertedNetwork = gson.fromJson(JsonNetwork, type);
    }
    return ConvertedNetwork;
  }
  
  private HashSet<NodeInfo> getNetwork()
  {
    HashSet<NodeInfo> ConvertedNetwork =null;
    Response answer= Service.path("GatewayServices").path("Network").path("getNetwork")
    .request(MediaType.APPLICATION_JSON)
    .get();
    if(answer.getStatus()==Response.Status.ACCEPTED.getStatusCode())
    {
      Gson gson= new Gson();
      String JsonNetwork= answer.readEntity(String.class);      
      java.lang.reflect.Type type = new TypeToken<HashSet<NodeInfo>>(){}.getType();
      ConvertedNetwork = gson.fromJson(JsonNetwork, type);
    }
    return ConvertedNetwork;
  }
 
  private static String getReadableTime(long millis)
  {
    long hours = TimeUnit.MILLISECONDS.toHours(millis);
    millis -= TimeUnit.HOURS.toMillis(hours);
    long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
    millis -= TimeUnit.MINUTES.toMillis(minutes);
    long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
   
    return String.format("%02d: %02d: %02d",hours,minutes,seconds);
  }
  
  private class SensorThread extends Thread
  {
    @Override
    public void run()
    {
      Gson gson= new Gson();
      
      try
      {  
        while(true)
        {
          Socket ConnectionSocket = InputSocket.accept();
          BufferedReader InputStream = new BufferedReader(new InputStreamReader(ConnectionSocket.getInputStream()));
          String JsonNodeMessage =InputStream.readLine();          
          ClientMessage Message= gson.fromJson(JsonNodeMessage,ClientMessage.class);
            
          synchronized(SensorJTable)
          {
            if(Message.getType()==ClientMessage.ClientMessageType.IN)
            {
              NodeInfo Node = Message.getNode();
              DefaultTableModel model = (DefaultTableModel) SensorJTable.getModel();
              IDRow.put(Node.getID(),SensorJTable.getRowCount());
              model.addRow(new Object[]{Node.getID(), Node.getSensorType().name(), getReadableTime(Node.getStartTime()),"Active"}); 
            }
            else if(Message.getType()==ClientMessage.ClientMessageType.OUT)
            {
              NodeInfo Node = Message.getNode();
              DefaultTableModel model = (DefaultTableModel) SensorJTable.getModel();
              model.setValueAt("Inactive",IDRow.get(Node.getID()),STATUS_COLUMN);    
            }
          }
          ConnectionSocket.close();
        }
      }      
      catch (IOException ex)
      {System.out.println("Socket Closed");} 
    }
  }
  
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {

    GetAllActiveSensorsJButton = new javax.swing.JButton();
    SingleAverageJButton = new javax.swing.JButton();
    jScrollPane1 = new javax.swing.JScrollPane();
    SingleMinimumJButton = new javax.swing.JButton();
    SingleMaximumJButton = new javax.swing.JButton();
    TemperatureCheckBox = new javax.swing.JCheckBox();
    LightCheckBox = new javax.swing.JCheckBox();
    AccelerometerCheckBox = new javax.swing.JCheckBox();
    jLabel5 = new javax.swing.JLabel();
    jLabel6 = new javax.swing.JLabel();
    jLabel7 = new javax.swing.JLabel();
    TotalAverageJButton = new javax.swing.JButton();
    TotalMaximumJButton = new javax.swing.JButton();
    TotalMinimumJButton = new javax.swing.JButton();
    jLabel8 = new javax.swing.JLabel();
    jLabel9 = new javax.swing.JLabel();
    jScrollPane2 = new javax.swing.JScrollPane();
    QueryResultsJTextArea = new javax.swing.JTextArea();
    jLabel14 = new javax.swing.JLabel();
    T1JTextField = new javax.swing.JTextField();
    T2JTextField = new javax.swing.JTextField();
    LastJButton = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

    GetAllActiveSensorsJButton.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
    GetAllActiveSensorsJButton.setText("Get All Active Sensors");
    GetAllActiveSensorsJButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        GetAllActiveSensorsJButtonActionPerformed(evt);
      }
    });

    SingleAverageJButton.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
    SingleAverageJButton.setText("Average");
    SingleAverageJButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        SingleAverageJButtonActionPerformed(evt);
      }
    });

    SensorJTable.setModel(new javax.swing.table.DefaultTableModel(
      new Object [][]
      {
      },
      new String []
      {
        "ID", "Type", "Start Time", "Status"
      }
    ));
    jScrollPane1.setViewportView(SensorJTable);

    SingleMinimumJButton.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
    SingleMinimumJButton.setText("Minimum");
    SingleMinimumJButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        SingleMinimumJButtonActionPerformed(evt);
      }
    });

    SingleMaximumJButton.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
    SingleMaximumJButton.setText("Maximum");
    SingleMaximumJButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        SingleMaximumJButtonActionPerformed(evt);
      }
    });

    TemperatureCheckBox.setText("Temperature");
    TemperatureCheckBox.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        TemperatureCheckBoxActionPerformed(evt);
      }
    });

    LightCheckBox.setText("Light");
    LightCheckBox.setToolTipText("");
    LightCheckBox.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        LightCheckBoxActionPerformed(evt);
      }
    });

    AccelerometerCheckBox.setText("Accelerometer");
    AccelerometerCheckBox.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        AccelerometerCheckBoxActionPerformed(evt);
      }
    });

    jLabel5.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
    jLabel5.setText("T1");

    jLabel6.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
    jLabel6.setText("Multiple Sensor");
    jLabel6.addContainerListener(new java.awt.event.ContainerAdapter()
    {
      public void componentRemoved(java.awt.event.ContainerEvent evt)
      {
        jLabel6ComponentRemoved(evt);
      }
    });

    jLabel7.setFont(new java.awt.Font("Dialog", 3, 14)); // NOI18N
    jLabel7.setText("HH:MM:SS");

    TotalAverageJButton.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
    TotalAverageJButton.setText("Average");
    TotalAverageJButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        TotalAverageJButtonActionPerformed(evt);
      }
    });

    TotalMaximumJButton.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
    TotalMaximumJButton.setText("Maximum");
    TotalMaximumJButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        TotalMaximumJButtonActionPerformed(evt);
      }
    });

    TotalMinimumJButton.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
    TotalMinimumJButton.setText("Minimum");
    TotalMinimumJButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        TotalMinimumJButtonActionPerformed(evt);
      }
    });

    jLabel8.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
    jLabel8.setText("Query Results");

    jLabel9.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
    jLabel9.setText("T2");

    QueryResultsJTextArea.setColumns(20);
    QueryResultsJTextArea.setRows(5);
    jScrollPane2.setViewportView(QueryResultsJTextArea);

    jLabel14.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
    jLabel14.setText("  Single Sensor");

    T1JTextField.setText("00:00:00");
    T1JTextField.setToolTipText("");
    T1JTextField.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        T1JTextFieldActionPerformed(evt);
      }
    });

    T2JTextField.setText("00:00:00");
    T2JTextField.setToolTipText("");
    T2JTextField.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        T2JTextFieldActionPerformed(evt);
      }
    });

    LastJButton.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
    LastJButton.setText("Last");
    LastJButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        LastJButtonActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(TemperatureCheckBox)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                      .addComponent(TotalMaximumJButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                      .addComponent(TotalAverageJButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                      .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(TotalMinimumJButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addComponent(AccelerometerCheckBox)
                  .addComponent(LightCheckBox)))
              .addComponent(jLabel9)
              .addComponent(jLabel5)))
          .addGroup(layout.createSequentialGroup()
            .addGap(64, 64, 64)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
              .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(T1JTextField)
              .addComponent(T2JTextField))))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 45, Short.MAX_VALUE)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(SingleAverageJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                .addComponent(SingleMaximumJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(SingleMinimumJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE))
              .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(GetAllActiveSensorsJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(168, 168, 168))
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addGroup(layout.createSequentialGroup()
                .addGap(43, 43, 43)
                .addComponent(jLabel8))
              .addComponent(LastJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
              .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 492, Short.MAX_VALUE)
              .addComponent(jScrollPane2))
            .addContainerGap(24, Short.MAX_VALUE))))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addGap(16, 16, 16)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(TotalAverageJButton)
          .addComponent(SingleAverageJButton))
        .addGap(18, 18, 18)
        .addComponent(TotalMaximumJButton)
        .addGap(18, 18, 18)
        .addComponent(TotalMinimumJButton)
        .addGap(22, 22, 22)
        .addComponent(TemperatureCheckBox)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(LightCheckBox)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(AccelerometerCheckBox)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
              .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(T1JTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
              .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(T2JTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap())
          .addGroup(layout.createSequentialGroup()
            .addGap(31, 31, 31)
            .addComponent(jLabel7)
            .addGap(98, 98, 98))))
      .addGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addGap(116, 116, 116)
            .addComponent(SingleMaximumJButton)
            .addGap(18, 18, 18)
            .addComponent(SingleMinimumJButton)
            .addGap(18, 18, 18)
            .addComponent(LastJButton))
          .addGroup(layout.createSequentialGroup()
            .addGap(9, 9, 9)
            .addComponent(GetAllActiveSensorsJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)))
        .addGap(18, 18, 18)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jScrollPane2)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(0, 0, Short.MAX_VALUE)))
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

    private void GetAllActiveSensorsJButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_GetAllActiveSensorsJButtonActionPerformed
    {//GEN-HEADEREND:event_GetAllActiveSensorsJButtonActionPerformed
      HashSet<NodeInfo> Network=getNetwork();
      if(Network.isEmpty())
        QueryResultsJTextArea.setText("\nNo Active Sensor");
      else
      {
        String StringNetwork="\n";
        for(NodeInfo Node: Network)
        {
          String StringUpTime=getReadableTime(Node.getUpTime());
          StringNetwork=StringNetwork+Node+" UPTIME "+StringUpTime+"\n";
        }
        QueryResultsJTextArea.setText(StringNetwork);
      }
    }//GEN-LAST:event_GetAllActiveSensorsJButtonActionPerformed
    
    private void SingleAverageJButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_SingleAverageJButtonActionPerformed
    {//GEN-HEADEREND:event_SingleAverageJButtonActionPerformed
      executeGatewayQuery("getSingleAverage");
    }//GEN-LAST:event_SingleAverageJButtonActionPerformed

    private void SingleMinimumJButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_SingleMinimumJButtonActionPerformed
    {//GEN-HEADEREND:event_SingleMinimumJButtonActionPerformed
      executeGatewayQuery("getSingleMinimum");
    }//GEN-LAST:event_SingleMinimumJButtonActionPerformed
 
    private void SingleMaximumJButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_SingleMaximumJButtonActionPerformed
    {//GEN-HEADEREND:event_SingleMaximumJButtonActionPerformed
      executeGatewayQuery("getSingleMaximum");
    }//GEN-LAST:event_SingleMaximumJButtonActionPerformed

    private void TemperatureCheckBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_TemperatureCheckBoxActionPerformed
    {//GEN-HEADEREND:event_TemperatureCheckBoxActionPerformed
      LightCheckBox.setSelected(false);
      AccelerometerCheckBox.setSelected(false);
    }//GEN-LAST:event_TemperatureCheckBoxActionPerformed

    private void LightCheckBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_LightCheckBoxActionPerformed
    {//GEN-HEADEREND:event_LightCheckBoxActionPerformed
      TemperatureCheckBox.setSelected(false);
      AccelerometerCheckBox.setSelected(false);
    }//GEN-LAST:event_LightCheckBoxActionPerformed

    private void AccelerometerCheckBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_AccelerometerCheckBoxActionPerformed
    {//GEN-HEADEREND:event_AccelerometerCheckBoxActionPerformed
     LightCheckBox.setSelected(false);
     TemperatureCheckBox.setSelected(false);
    }//GEN-LAST:event_AccelerometerCheckBoxActionPerformed

  private void jLabel6ComponentRemoved(java.awt.event.ContainerEvent evt)//GEN-FIRST:event_jLabel6ComponentRemoved
  {//GEN-HEADEREND:event_jLabel6ComponentRemoved
    // TODO add your handling code here:
  }//GEN-LAST:event_jLabel6ComponentRemoved

  private void TotalAverageJButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_TotalAverageJButtonActionPerformed
  {//GEN-HEADEREND:event_TotalAverageJButtonActionPerformed
    executeGatewayQuery("getTotalAverage");
  }//GEN-LAST:event_TotalAverageJButtonActionPerformed

  private void TotalMaximumJButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_TotalMaximumJButtonActionPerformed
  {//GEN-HEADEREND:event_TotalMaximumJButtonActionPerformed
    executeGatewayQuery("getTotalMaximum");
  }//GEN-LAST:event_TotalMaximumJButtonActionPerformed

  private void TotalMinimumJButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_TotalMinimumJButtonActionPerformed
  {//GEN-HEADEREND:event_TotalMinimumJButtonActionPerformed
    executeGatewayQuery("getTotalMinimum");
  }//GEN-LAST:event_TotalMinimumJButtonActionPerformed

  private void T1JTextFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_T1JTextFieldActionPerformed
  {//GEN-HEADEREND:event_T1JTextFieldActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_T1JTextFieldActionPerformed

  private void T2JTextFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_T2JTextFieldActionPerformed
  {//GEN-HEADEREND:event_T2JTextFieldActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_T2JTextFieldActionPerformed

  private void LastJButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_LastJButtonActionPerformed
  {//GEN-HEADEREND:event_LastJButtonActionPerformed
    getLastMeasurement();
  }//GEN-LAST:event_LastJButtonActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JCheckBox AccelerometerCheckBox;
  private javax.swing.JButton GetAllActiveSensorsJButton;
  private javax.swing.JButton LastJButton;
  private javax.swing.JCheckBox LightCheckBox;
  private javax.swing.JTextArea QueryResultsJTextArea;
  private final javax.swing.JTable SensorJTable = new javax.swing.JTable();
  private javax.swing.JButton SingleAverageJButton;
  private javax.swing.JButton SingleMaximumJButton;
  private javax.swing.JButton SingleMinimumJButton;
  private javax.swing.JTextField T1JTextField;
  private javax.swing.JTextField T2JTextField;
  private javax.swing.JCheckBox TemperatureCheckBox;
  private javax.swing.JButton TotalAverageJButton;
  private javax.swing.JButton TotalMaximumJButton;
  private javax.swing.JButton TotalMinimumJButton;
  private javax.swing.JLabel jLabel14;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JLabel jLabel6;
  private javax.swing.JLabel jLabel7;
  private javax.swing.JLabel jLabel8;
  private javax.swing.JLabel jLabel9;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JScrollPane jScrollPane2;
  // End of variables declaration//GEN-END:variables

}
