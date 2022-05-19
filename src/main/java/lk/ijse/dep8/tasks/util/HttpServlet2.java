package lk.ijse.dep8.tasks.util;

import com.sun.deploy.net.HttpRequest;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpServlet2 extends HttpServlet {
    private Logger logger =Logger.getLogger(HttpServlet2.class.getName());
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try{
            super.service(req,resp);
    }catch (Throwable t){
            logger.severe("BAD");
            logger.logp(Level.SEVERE,t.getStackTrace()[0].getClassName(),t.getStackTrace()[0].getMethodName(),t.getMessage(),t);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            resp.setContentType("application/json");

            /////////////////




            //////////////////


            HttpResponseErrorMassage errorMassage =null;
            if (t instanceof ResponseStatusException){
                ResponseStatusException res = (ResponseStatusException) t;
                resp.setStatus(res.getStatus());
                errorMassage = new HttpResponseErrorMassage(new Date().getTime(),
                        res.getStatus(),
                        sw.toString(), t.getMessage(), req.getRequestURI());
            }else{
                errorMassage = new HttpResponseErrorMassage(new Date().getTime(),
                        500,
                        sw.toString(), t.getMessage(), req.getRequestURI());
            }


            Jsonb jsonb = JsonbBuilder.create();
            jsonb.toJson(errorMassage, resp.getWriter());

        }
    }
}
