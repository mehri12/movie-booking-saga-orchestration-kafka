import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BookingRequest {
  userId: string;
  showId: string;
  seatIds: string[];
  amount: number;
}

export interface BookingResponse {
  reservationId: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly http = inject(HttpClient);

  // Adresse relative : la page et l'API partagent la même origine.
  // En développement, proxy.conf.json redirige vers l'Ingress.
  // Dans le cluster, c'est l'Ingress qui route /booking-service.
  private readonly baseUrl = '/booking-service';

  bookSeats(request: BookingRequest): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(`${this.baseUrl}/bookSeat`, request);
  }

  getBooking(reservationId: string): Observable<BookingResponse> {
    return this.http.get<BookingResponse>(
      `${this.baseUrl}/bookings/${reservationId}`,
    );
  }
}