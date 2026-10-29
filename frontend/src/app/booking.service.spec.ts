import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { BookingService } from './booking.service';

describe('BookingService', () => {
  let service: BookingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BookingService);
    http = TestBed.inject(HttpTestingController);
  });

  // Échoue si une requête prévue n'a pas été consommée par le test.
  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should send the booking with POST /booking-service/bookSeat', () => {
    const request = {
      userId: 'user1',
      showId: 'SHOW_101',
      seatIds: ['A1'],
      amount: 1000,
    };

    service.bookSeats(request).subscribe((response) => {
      expect(response.reservationId).toBe('abc123');
      expect(response.status).toBe('PENDING');
    });

    const call = http.expectOne('/booking-service/bookSeat');
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
    call.flush({ reservationId: 'abc123', status: 'PENDING' });
  });

  it('should read a booking with GET /booking-service/bookings/{id}', () => {
    service.getBooking('abc123').subscribe((response) => {
      expect(response.status).toBe('CONFIRMED');
    });

    const call = http.expectOne('/booking-service/bookings/abc123');
    expect(call.request.method).toBe('GET');
    call.flush({ reservationId: 'abc123', status: 'CONFIRMED' });
  });
});