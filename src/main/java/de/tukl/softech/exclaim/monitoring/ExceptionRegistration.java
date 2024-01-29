package de.tukl.softech.exclaim.monitoring;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.tomcat.util.http.fileupload.FileUploadBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@ControllerAdvice
public class ExceptionRegistration {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionRegistration.class);
    private MetricsService metrics;

    public ExceptionRegistration(MetricsService metrics) {
        this.metrics = metrics;
    }

    @ExceptionHandler(Exception.class)
    public void registerException(HttpServletRequest request, Exception e) throws Exception {
        if (e instanceof AccessDeniedException) {
            String username = request.getUserPrincipal().getName();
            String uri = request.getRequestURI();
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String[]> paramEntry : request.getParameterMap().entrySet()) {
                builder.append(paramEntry.getKey()).append("=").append(String.join(", ", paramEntry.getValue()));
                builder.append(System.lineSeparator());
            }
            logger.warn("Access denied to {}\nparameters:\n{}\n by user {}", uri, builder.toString(), username);
        } else if (e instanceof ClientAbortException) {
            logger.warn("Connection reset by client: Path {}, client: {}", request.getRequestURI(), request.getHeader("User-Agent"));
        } else if (e instanceof MultipartException &&
                e.getCause() instanceof IllegalStateException &&
                e.getCause().getCause() instanceof FileUploadBase.SizeLimitExceededException) {
            FileUploadBase.SizeLimitExceededException limitExceededException = (FileUploadBase.SizeLimitExceededException) e.getCause().getCause();
            logger.warn("Upload size limit exceeded: {} instead of {} bytes", limitExceededException.getActualSize(), limitExceededException.getPermittedSize());
        } else if (e instanceof MessageDeliveryException
                && e.getCause() instanceof InvalidCsrfTokenException) {
            logger.warn("Invalid CSRF token", e);
        } else {
            logger.error("Logging exception " + e + " for path "
                    + request.getRequestURI()
                    + ", client "
                    +request.getHeader("User-Agent"),
                    e);
            metrics.registerException(e);
        }
        throw e;
    }
}
