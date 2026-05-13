package tokai.com.mx.SIGMAV2.shared.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CustomResponse <T> {

    T data;
    Boolean error;
    int statusCode;
    String message;


    public CustomResponse(T data, boolean error, int statusCode) {
        this.data = data;
        this.error = error;
        this.statusCode = statusCode;
    }


    public CustomResponse(boolean error, int statusCode, String message) {
        this.data = null;
        this.error = error;
        this.statusCode = statusCode;
        this.message = message;
    }

    public boolean isError() {
        return error;
    }
}