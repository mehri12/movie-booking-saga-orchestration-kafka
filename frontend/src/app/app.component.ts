import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { BookingService } from './booking.service';

interface Show {
  id: string;
  title: string;
  seats: string[];
}

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  private readonly bookingService = inject(BookingService);

  readonly shows: Show[] = [
    { id: 'SHOW_101', title: 'Salle 1 (SHOW_101)', seats: ['A1', 'A2', 'A3', 'A4', 'A5'] },
    { id: 'SHOW_202', title: 'Salle 2 (SHOW_202)', seats: ['B1', 'B2', 'B3', 'B4', 'B5'] },
    { id: 'SHOW_303', title: 'Salle 3 (SHOW_303)', seats: ['C1', 'C2', 'C3', 'C4', 'C5'] },
  ];

  // Valeurs du formulaire
  userId = 'user1';
  showId = this.shows[0].id;
  amount = 1000;
  selectedSeats: string[] = [];

  // État de la réservation en cours
  inProgress = false;
  reservationId = '';
  status = '';
  errorMessage = '';

  // Ce qui a été réservé (le billet ne change pas si on modifie le formulaire ensuite)
  bookedShowTitle = '';
  bookedSeats: string[] = [];
  bookedUser = '';
  bookedAmount = 0;

  get seats(): string[] {
    return this.shows.find((show) => show.id === this.showId)?.seats ?? [];
  }

  get isFinal(): boolean {
    return this.status === 'CONFIRMED' || this.status === 'FAILED';
  }

  get bookLabel(): string {
    if (this.inProgress) {
      return 'Réservation en cours…';
    }
    const count = this.selectedSeats.length;
    if (count === 0) {
      return 'Réserver';
    }
    return count === 1 ? 'Réserver 1 siège' : `Réserver ${count} sièges`;
  }

  get statusLabel(): string {
    switch (this.status) {
      case 'CONFIRMED':
        return 'Confirmée';
      case 'FAILED':
        return 'Refusée';
      default:
        return 'En cours';
    }
  }

  get statusMessage(): string {
    switch (this.status) {
      case 'CONFIRMED':
        return 'Réservation confirmée. Les sièges sont à vous.';
      case 'FAILED':
        return 'Réservation refusée : un siège est déjà pris ou le paiement a été refusé.';
      default:
        return 'Vérification des sièges et du paiement en cours.';
    }
  }

  onShowChange(showId: string): void {
    this.showId = showId;
    this.selectedSeats = [];
  }

  isSelected(seat: string): boolean {
    return this.selectedSeats.includes(seat);
  }

  toggleSeat(seat: string): void {
    this.selectedSeats = this.isSelected(seat)
      ? this.selectedSeats.filter((s) => s !== seat)
      : [...this.selectedSeats, seat];
  }

  async book(): Promise<void> {
    this.errorMessage = '';
    this.reservationId = '';
    this.status = '';

    if (this.selectedSeats.length === 0) {
      this.errorMessage = 'Choisissez au moins un siège.';
      return;
    }

    this.inProgress = true;
    try {
      const created = await firstValueFrom(
        this.bookingService.bookSeats({
          userId: this.userId,
          showId: this.showId,
          seatIds: this.selectedSeats,
          amount: this.amount,
        }),
      );

      this.bookedShowTitle =
        this.shows.find((show) => show.id === this.showId)?.title ?? this.showId;
      this.bookedSeats = [...this.selectedSeats];
      this.bookedUser = this.userId;
      this.bookedAmount = this.amount;
      this.reservationId = created.reservationId;
      this.status = created.status;

      await this.followStatus(created.reservationId);
    } catch {
      this.errorMessage =
        "L'API ne répond pas. Vérifiez que minikube est démarré et que « minikube tunnel » est ouvert.";
    } finally {
      this.inProgress = false;
    }
  }

  // La saga est asynchrone : on interroge l'API toutes les 2 secondes
  // jusqu'à un statut final (CONFIRMED ou FAILED), 60 secondes au maximum.
  private async followStatus(reservationId: string): Promise<void> {
    if (this.isFinal) {
      return;
    }
    for (let attempt = 0; attempt < 30; attempt++) {
      await new Promise((resolve) => setTimeout(resolve, 2000));
      const current = await firstValueFrom(
        this.bookingService.getBooking(reservationId),
      );
      this.status = current.status;
      if (this.isFinal) {
        return;
      }
    }
    this.errorMessage =
      "Le résultat n'est pas arrivé après 60 secondes. Vérifiez que les services tournent dans le cluster.";
  }
}