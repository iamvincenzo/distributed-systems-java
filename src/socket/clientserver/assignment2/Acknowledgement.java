package socket.clientserver.assignment2;

import java.io.Serializable;

/**
*
* The class {@code Acknowledgement} provides a model for the server to communicate to 
* the client if the purchase is successfully done or not.
*
**/
public class Acknowledgement implements Serializable
{
  private static final long serialVersionUID = 1L;
  private boolean value;

  /**
   * 
   * Class constructor.
   *   
  **/
  public Acknowledgement(){}

  /**
   * Gets the value.
   *
   * @return the value.
   *
  **/
  public boolean getValue()
  {
    return this.value;
  }
  
  /**
   * Sets the value.
   *
   * @param  v  true if client can purchase the object, false otherwise.
   *
  **/
  public void setValue(boolean v)
  {
    this.value = v;
  }
}