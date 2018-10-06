package verify;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.Map;


/**
 * Created by tunde on 2017-07-19.
 */

public class SendRequest {
    
    private String NOTE = "Unable to verify details from server, please try again.";
    private int VERIFIED = 0; //0 -server error, 1- failed, 2 - verified, 3- expired
    private String FULLNAME ="--";
    private String FIRST_ISSUE = "--";
    private String EXP_DATE = "--";
    private String exp_message = "Expiry Date is less than or equal to 30 days, you can renew the drivers licence";

    public String getNote(){
        return this.NOTE;
    }
    public int getVerified(){
        return VERIFIED;
    }

    public String getFullname(){
        return FULLNAME;
    }
    public String getExpiryDate(){
        return EXP_DATE;
    }

    public String getFirstIssue(){
        return FIRST_ISSUE;
    }
    
    public void getRequest(String licenceNo, String day, String month, String year){
        VERIFIED = 0;
        System.setProperty("jsse.enableSNIExtension", "false");
        Document doc = null;
        try {

            Connection.Response res1 = Jsoup.connect("https://www.nigeriadriverslicence.org/dlApplication/reissue").timeout(50 * 1000).ignoreHttpErrors(true).validateTLSCertificates(false).method(Connection.Method.GET).execute();
            Document welcomePage = res1.parse();
            Map welcomCookies = res1.cookies();

            //get csrf token first
            String csrf = welcomePage.getElementById("search_reissue__csrf_token").val();
            // System.out.println(csrf);

            doc = Jsoup.connect("https://www.nigeriadriverslicence.org/dlApplication/reissue")
                    .timeout(50 * 1000)
                    .data("search_reissue[dl_number]", licenceNo)
                    .data("search_reissue[dob][day]", day)
                    .data("search_reissue[dob][month]", month)
                    .data("search_reissue[dob][year]", year)
                    .cookies(welcomCookies)
                    .data("search_reissue[_csrf_token]", csrf)
                    .data("submit", "Search")
                    // and other hidden fields which are being passed in post request.
                    .userAgent("Mozilla/5.0 (Windows NT 6.2; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0")
                    .ignoreHttpErrors(true).validateTLSCertificates(false)
                    .post();

        } catch (Exception e) {
            e.printStackTrace();
            //error connecting
        }

        //extract the data
        extractData(doc);

    }

    private void extractData(Document doc){
        try {

            Element divTag = doc.getElementById("flash_error");
            Elements ulTag = doc.getElementsByClass("error_list");
            //System.out.println(doc.body());


            if (divTag != null || ulTag.iterator().hasNext()) {
                boolean hasError = false;
                if (divTag != null) {
                    hasError = divTag.hasClass("error_list");
                }

                if (hasError) {
                    NOTE = divTag.select("span").first().text();
                } else {
                    //if the last element is the first element (csrf code issue / other errors) we dont want to show "Required"
                    if (!ulTag.last().select("li").first().text().equals("Required.")) {
                        NOTE = ulTag.last().select("li").first().text();
                    }
                }
                VERIFIED = 1;
            } else {
                divTag = doc.getElementById("tbl_dl_application_surname");
                if (divTag != null) {
                    String firstissue = doc.getElementById("tbl_dl_application_first_issued_date").val();
                    String firstname = doc.getElementById("tbl_dl_application_surname").val();
                    String lastname = doc.getElementById("tbl_dl_application_firstname").val();
                    String exp_date = doc.getElementById("tbl_dl_application_expiry_date").val();

                    FULLNAME = firstname + " " + lastname;
                    FIRST_ISSUE = firstissue;
                    NOTE = "Licence Verified";
                    EXP_DATE = exp_date;
                    VERIFIED = 2;
                } else {
                    NOTE = "Unable to verify license information";
                    VERIFIED = 1;
                }
            }
            // set verified based on note
            if(NOTE.toLowerCase().trim().equals(exp_message.toLowerCase().trim()) ){
                VERIFIED = 3; //expired
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
