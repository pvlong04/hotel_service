export const apiModules = [
  {
    name: 'Auth',
    endpoints: [
      {
        name: 'Register',
        method: 'POST',
        path: '/auth/register',
        bodySample: {
          username: 'guestuser01',
          email: 'guest@example.com',
          password: '123456',
          fullName: 'Guest User',
          phone: '0900000000',
        },
      },
      {
        name: 'Login',
        method: 'POST',
        path: '/auth/login',
        bodySample: {
          usernameOrEmail: 'guest@example.com',
          password: '123456',
        },
      },
      {
        name: 'Refresh token',
        method: 'POST',
        path: '/auth/refresh',
        bodySample: {
          refreshToken: 'paste_refresh_token_here',
        },
      },
      {
        name: 'Logout',
        method: 'POST',
        path: '/auth/logout',
        bodySample: {
          refreshToken: 'paste_refresh_token_here',
        },
      },
    ],
  },
  {
    name: 'Users',
    endpoints: [
      {
        name: 'Get users',
        method: 'GET',
        path: '/users',
        auth: true,
        querySample: {
          keyword: '',
          status: 'ACTIVE',
          page: 0,
          size: 10,
        },
      },
      {
        name: 'Get my profile',
        method: 'GET',
        path: '/users/me',
        auth: true,
      },
      {
        name: 'Get user by id',
        method: 'GET',
        path: '/users/:userId',
        auth: true,
        pathSample: { userId: 1 },
      },
    ],
  },
  {
    name: 'Hotels',
    endpoints: [
      {
        name: 'Search hotels',
        method: 'POST',
        path: '/hotels/search',
        bodySample: {
          keyword: 'Da Nang',
          page: 0,
          size: 10,
        },
      },
      {
        name: 'Get hotel by id',
        method: 'GET',
        path: '/hotels/:hotelId',
        pathSample: { hotelId: 1 },
      },
      {
        name: 'Create hotel',
        method: 'POST',
        path: '/hotels',
        auth: true,
        bodySample: {
          name: 'Hotel Demo',
          address: '123 Street',
          phone: '0900000000',
          email: 'hotel@example.com',
          description: 'Demo hotel',
          starRating: 4,
          timezone: 'Asia/Ho_Chi_Minh',
          status: 'ACTIVE',
        },
      },
    ],
  },
  {
    name: 'Rooms',
    endpoints: [
      {
        name: 'Get rooms',
        method: 'GET',
        path: '/rooms',
        querySample: {
          hotelId: 1,
          status: 'AVAILABLE',
          page: 0,
          size: 10,
        },
      },
      {
        name: 'Get room by id',
        method: 'GET',
        path: '/rooms/:id',
        pathSample: { id: 1 },
      },
      {
        name: 'Get available rooms',
        method: 'GET',
        path: '/rooms/available',
        querySample: {
          hotelId: 1,
          checkIn: '2026-04-01',
          checkOut: '2026-04-02',
        },
      },
      {
        name: 'Create room',
        method: 'POST',
        path: '/rooms',
        auth: true,
        bodySample: {
          hotelId: 1,
          roomNumber: '101',
          roomTypeId: 1,
          floorId: 1,
          status: 'AVAILABLE',
          note: 'Near elevator',
        },
      },
    ],
  },
  {
    name: 'Reservations',
    endpoints: [
      {
        name: 'Check availability',
        method: 'POST',
        path: '/reservations/availability',
        bodySample: {
          hotelId: 1,
          checkInDate: '2026-04-01',
          checkOutDate: '2026-04-02',
          adults: 1,
          children: 0,
        },
      },
      {
        name: 'Create reservation',
        method: 'POST',
        path: '/reservations',
        auth: true,
        bodySample: {
          hotelId: 1,
          checkInDate: '2026-04-01',
          checkOutDate: '2026-04-03',
          adultCount: 2,
          childCount: 0,
          specialRequests: 'High floor',
          rooms: [{ roomTypeId: 1 }],
        },
      },
      {
        name: 'My reservations',
        method: 'GET',
        path: '/reservations/me',
        auth: true,
      },
      {
        name: 'Update reservation status',
        method: 'PATCH',
        path: '/reservations/:reservationId/status',
        auth: true,
        pathSample: { reservationId: 1 },
        bodySample: {
          status: 'CONFIRMED',
          internalNotes: 'Confirmed by staff',
        },
      },
      {
        name: 'Create payment',
        method: 'POST',
        path: '/reservations/payments',
        auth: true,
        bodySample: {
          reservationId: 1,
          amount: 1000000,
          method: 'CARD',
          provider: 'VISA',
          providerTransId: 'TX123',
          note: 'Paid online',
        },
      },
      {
        name: 'Create charge',
        method: 'POST',
        path: '/reservations/charges',
        auth: true,
        bodySample: {
          reservationId: 1,
          chargeType: 'MINIBAR',
          amount: 200000,
          description: '2 drinks',
        },
      },
    ],
  },
]
