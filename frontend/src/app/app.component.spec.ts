import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the page title', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const title = fixture.nativeElement.querySelector('h1');
    expect(title?.textContent).toContain('Réservation');
  });

  it('should ask for at least one seat before booking', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    await fixture.componentInstance.book();
    expect(fixture.componentInstance.errorMessage).toBe(
      'Choisissez au moins un siège.',
    );
  });
});