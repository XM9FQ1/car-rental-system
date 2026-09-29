package hu.nye.carrental.service;

// İş kuralı ihlal edildiğinde fırlatılır (ör. araç müsait değil)
public class RentalException extends RuntimeException {

    public RentalException(String message) {
        super(message);
    }
}
