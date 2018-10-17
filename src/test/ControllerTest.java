package test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.gen5.api.DisplayName;
import verify.SendRequest;

import static org.junit.gen5.api.Assertions.assertEquals;


public class ControllerTest {
    SendRequest sr;
    @Before
    public void initClass(){
        sr = new SendRequest();
    }

    @Test
    @DisplayName("Test Licence 1")
    public void checkLicence(){
        sr.getRequest("234252555","13","12", "1988");
        int verified = sr.getVerified();
        assertEquals(1, verified, "Didn't get 1");
    }

    @After
    public void cleanState(){
        sr = null;
    }
}
