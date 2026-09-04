import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { mockTokenInterceptor } from './mock-token.interceptor';
import { CurrentRoleService, DEMO_ROLES } from '../services/current-role.service';

describe('mockTokenInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let currentRole: CurrentRoleService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([mockTokenInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    currentRole = TestBed.inject(CurrentRoleService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('attaches Authorization: Bearer <mock token> for the default role', () => {
    httpClient.get('/api/applications').subscribe();

    const request = httpTesting.expectOne('/api/applications');
    expect(request.request.headers.get('Authorization')).toBe(
      `Bearer ${DEMO_ROLES[0].mockToken}`,
    );
    request.flush({});
  });

  it('attaches the token for the currently selected role, not always the default', () => {
    currentRole.setRole('ROLE-003');

    httpClient.get('/api/applications').subscribe();

    const request = httpTesting.expectOne('/api/applications');
    expect(request.request.headers.get('Authorization')).toBe('Bearer mock-role-003');
    request.flush({});
  });
});
