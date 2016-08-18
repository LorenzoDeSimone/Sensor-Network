//Lorenzo De Simone N880090

package client;

import ClientInfo.ClientInfo;
import static client.Client.centerWindow;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;

public class LoginMenu extends javax.swing.JFrame
{  
  
  public LoginMenu()
  {
    initComponents();
    centerWindow(this); 
    setResizable(false);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setVisible(true);
  }
  
  private void login() throws IOException
  {          
    JerseyClient Client;
    JerseyWebTarget Service;      
    Client = JerseyClientBuilder.createClient();
    String ServerIPAddress;
    String Username;
    ServerSocket InputSocket;
 
    ServerIPAddress=IPAddressJTextField.getText();
    if(ServerIPAddress.equals(""))
    {
      JOptionPane.showMessageDialog(null, "Server Address Field cannot be empty");
      return;
    }
    
    Username = UsernameJTextField.getText();
    if(Username.equals(""))
    {
      JOptionPane.showMessageDialog(null, "Username Field cannot be empty");
      return;
    }
    
    InputSocket=new ServerSocket(0);

    Service = Client.target(UriBuilder.fromUri(ServerIPAddress).build());
    Gson gson= new Gson();
    ClientInfo NewClient= new ClientInfo(Username,""+InputSocket.getLocalPort(),InetAddress.getLocalHost().getHostAddress());
    String JsonClientInfo = gson.toJson(NewClient);
    Response Answer;
    try
    {
      Answer= Service.path("GatewayServices").path("Client").path("login")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.entity(JsonClientInfo,MediaType.APPLICATION_JSON));
    }
    catch(ProcessingException ex)
    {
      InputSocket.close();
      JOptionPane.showMessageDialog(null, "Malformed Network Address");
      return;
    }
    
    if(Answer.getStatus()==Response.Status.ACCEPTED.getStatusCode())
    {
      this.dispose();
      InterrogationMenu Menu= new InterrogationMenu(NewClient,Service,InputSocket,Username);
    }
    else if(Answer.getStatus()==Response.Status.FORBIDDEN.getStatusCode())
    {
      InputSocket.close();
      JOptionPane.showMessageDialog(null, "Username already in use");
    }
    else if(Answer.getStatus()==Response.Status.NOT_FOUND.getStatusCode())
    {
      InputSocket.close();
      JOptionPane.showMessageDialog(null, "Invalid network address");
    }
    else
    {
      InputSocket.close();
      JOptionPane.showMessageDialog(null, "Unknown Error:\n"+Answer);
    }
  }

  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {

    jMenuItem1 = new javax.swing.JMenuItem();
    jFrame1 = new javax.swing.JFrame();
    LoginJButton = new javax.swing.JButton();
    UsernameJTextField = new javax.swing.JTextField();
    jLabel1 = new javax.swing.JLabel();
    jLabel3 = new javax.swing.JLabel();
    IPAddressJTextField = new javax.swing.JTextField();

    jMenuItem1.setText("jMenuItem1");

    javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
    jFrame1.getContentPane().setLayout(jFrame1Layout);
    jFrame1Layout.setHorizontalGroup(
      jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 400, Short.MAX_VALUE)
    );
    jFrame1Layout.setVerticalGroup(
      jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 300, Short.MAX_VALUE)
    );

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

    LoginJButton.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
    LoginJButton.setText("Login");
    LoginJButton.setToolTipText("");
    LoginJButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        LoginJButtonActionPerformed(evt);
      }
    });

    UsernameJTextField.setToolTipText("");
    UsernameJTextField.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        UsernameJTextFieldActionPerformed(evt);
      }
    });

    jLabel1.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
    jLabel1.setText("Username");

    jLabel3.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
    jLabel3.setText("Server  Address");

    IPAddressJTextField.setText("http://localhost:8080/Gateway");
    IPAddressJTextField.setToolTipText("");
    IPAddressJTextField.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        IPAddressJTextFieldActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap(29, Short.MAX_VALUE)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(LoginJButton)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
              .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
              .addComponent(IPAddressJTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE)
              .addComponent(UsernameJTextField))))
        .addGap(41, 41, 41))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(UsernameJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addGap(18, 18, 18)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(IPAddressJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addGap(18, 18, 18)
        .addComponent(LoginJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addGap(47, 47, 47))
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

    private void LoginJButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_LoginJButtonActionPerformed
    {//GEN-HEADEREND:event_LoginJButtonActionPerformed
      try
      {login();}
      catch (IOException ex)
      {Logger.getLogger(LoginMenu.class.getName()).log(Level.SEVERE, null, ex);}
    }//GEN-LAST:event_LoginJButtonActionPerformed

    private void UsernameJTextFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_UsernameJTextFieldActionPerformed
    {//GEN-HEADEREND:event_UsernameJTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_UsernameJTextFieldActionPerformed

    private void IPAddressJTextFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_IPAddressJTextFieldActionPerformed
    {//GEN-HEADEREND:event_IPAddressJTextFieldActionPerformed
       // TODO add your handling code here:
    }//GEN-LAST:event_IPAddressJTextFieldActionPerformed
    
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JTextField IPAddressJTextField;
  private javax.swing.JButton LoginJButton;
  private javax.swing.JTextField UsernameJTextField;
  private javax.swing.JFrame jFrame1;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JMenuItem jMenuItem1;
  // End of variables declaration//GEN-END:variables
    }
