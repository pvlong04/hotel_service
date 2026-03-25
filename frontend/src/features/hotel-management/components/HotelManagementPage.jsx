import { useMemo, useState } from 'react'
import { API_BASE_URL } from '../../../shared/config/env'
import { requestApi } from '../../../shared/lib/httpClient'
import { JsonBlock } from '../../../shared/ui/JsonBlock'

const TOKEN_KEY = 'hotel_service_api_token'
const USER_STATUSES = ['ACTIVE', 'PENDING', 'BANNED']

function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

function pickData(payload) {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return payload.data
  }
  return payload
}

function normalizeList(data) {
  if (Array.isArray(data)) return data
  if (data && Array.isArray(data.items)) return data.items
  if (data && Array.isArray(data.content)) return data.content
  return []
}

function formatDate(value) {
  if (!value) return 'N/A'
  return value
}

export function HotelManagementPage() {
  const [token, setToken] = useState(getStoredToken)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const [loginPayload, setLoginPayload] = useState({
    usernameOrEmail: 'guest@example.com',
    password: '123456',
  })

  const [bookingSearch, setBookingSearch] = useState({
    keyword: 'Da Nang',
    checkInDate: '2026-04-01',
    checkOutDate: '2026-04-03',
    adults: 2,
    children: 0,
    rooms: 1,
    page: 0,
    size: 6,
  })

  const [availability, setAvailability] = useState({
    hotelId: 1,
    checkInDate: '2026-04-01',
    checkOutDate: '2026-04-03',
    adults: 2,
    children: 0,
  })

  const [userFilter, setUserFilter] = useState({ keyword: '', status: 'ACTIVE', page: 0, size: 8 })

  const [hotels, setHotels] = useState([])
  const [availableRooms, setAvailableRooms] = useState([])
  const [users, setUsers] = useState([])
  const [myReservations, setMyReservations] = useState([])
  const [profile, setProfile] = useState(null)

  const [createReservationPayload, setCreateReservationPayload] = useState(`{
  "hotelId": 1,
  "checkInDate": "2026-04-01",
  "checkOutDate": "2026-04-03",
  "adultCount": 2,
  "childCount": 0,
  "specialRequests": "Ocean view",
  "rooms": [{ "roomTypeId": 1 }]
}`)
  const [createdReservation, setCreatedReservation] = useState(null)

  const stats = useMemo(
    () => [
      { label: 'Khách sạn nổi bật', value: hotels.length },
      { label: 'Phòng còn trống', value: availableRooms.length },
      { label: 'Đơn của tôi', value: myReservations.length },
      { label: 'Người dùng', value: users.length },
    ],
    [hotels.length, availableRooms.length, myReservations.length, users.length],
  )

  const callApi = async (executor, successMessage) => {
    setLoading(true)
    setError('')
    setMessage('')

    try {
      await executor()
      if (successMessage) setMessage(successMessage)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra')
    } finally {
      setLoading(false)
    }
  }

  const login = () =>
    callApi(async () => {
      const result = await requestApi({ method: 'POST', path: '/auth/login', body: loginPayload })
      if (!result.ok) throw new Error(`Đăng nhập thất bại (HTTP ${result.status})`)

      const data = pickData(result.data)
      const accessToken = data?.accessToken
      setProfile(data?.user ?? null)

      if (accessToken) {
        setToken(accessToken)
        localStorage.setItem(TOKEN_KEY, accessToken)
      }
    }, 'Đăng nhập thành công')

  const searchHotels = () =>
    callApi(async () => {
      const body = {
        keyword: bookingSearch.keyword,
        checkInDate: bookingSearch.checkInDate,
        checkOutDate: bookingSearch.checkOutDate,
        adults: Number(bookingSearch.adults),
        children: Number(bookingSearch.children),
        rooms: Number(bookingSearch.rooms),
        page: Number(bookingSearch.page),
        size: Number(bookingSearch.size),
      }

      const result = await requestApi({ method: 'POST', path: '/hotels/search', body })
      if (!result.ok) throw new Error(`Tìm khách sạn thất bại (HTTP ${result.status})`)

      const list = normalizeList(pickData(result.data))
      setHotels(list)

      const firstHotelId = list?.[0]?.hotelId || list?.[0]?.id
      if (firstHotelId) {
        setAvailability((prev) => ({ ...prev, hotelId: Number(firstHotelId) }))
      }
    }, 'Đã tìm thấy danh sách khách sạn phù hợp')

  const checkAvailability = () =>
    callApi(async () => {
      const result = await requestApi({
        method: 'POST',
        path: '/reservations/availability',
        body: {
          ...availability,
          hotelId: Number(availability.hotelId),
          adults: Number(availability.adults),
          children: Number(availability.children),
        },
      })

      if (!result.ok) throw new Error(`Kiểm tra phòng trống thất bại (HTTP ${result.status})`)
      setAvailableRooms(normalizeList(pickData(result.data)))
    }, 'Đã tải phòng còn trống')

  const fetchUsers = () =>
    callApi(async () => {
      const result = await requestApi({ method: 'GET', path: '/users', token, query: userFilter })
      if (!result.ok) throw new Error(`Lấy users thất bại (HTTP ${result.status})`)
      setUsers(normalizeList(pickData(result.data)))
    }, 'Đã tải danh sách người dùng')

  const fetchMyReservations = () =>
    callApi(async () => {
      const result = await requestApi({ method: 'GET', path: '/reservations/me', token })
      if (!result.ok) throw new Error(`Lấy reservations thất bại (HTTP ${result.status})`)
      setMyReservations(normalizeList(pickData(result.data)))
    }, 'Đã tải danh sách đặt phòng của tôi')

  const fetchMyProfile = () =>
    callApi(async () => {
      const result = await requestApi({ method: 'GET', path: '/users/me', token })
      if (!result.ok) throw new Error(`Lấy profile thất bại (HTTP ${result.status})`)
      setProfile(pickData(result.data))
    }, 'Đã tải profile')

  const createReservation = () =>
    callApi(async () => {
      let payload
      try {
        payload = JSON.parse(createReservationPayload)
      } catch {
        throw new Error('JSON tạo reservation không hợp lệ')
      }

      const result = await requestApi({ method: 'POST', path: '/reservations', token, body: payload })
      if (!result.ok) throw new Error(`Tạo reservation thất bại (HTTP ${result.status})`)
      setCreatedReservation(pickData(result.data))
    }, 'Tạo đặt phòng thành công')

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100">
      <div className="mx-auto max-w-7xl px-4 py-6">
        <header className="rounded-2xl border border-blue-400/20 bg-slate-900/90 p-4 shadow-2xl shadow-blue-500/10 backdrop-blur">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-sm uppercase tracking-[0.2em] text-blue-300">Sky Hotel Style</p>
              <h1 className="text-2xl font-bold text-white">Hotel Front-end Demo</h1>
              <p className="text-xs text-slate-400">API Base: {API_BASE_URL}</p>
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={fetchMyProfile}
                disabled={!token || loading}
                className="rounded-full border border-blue-300/40 px-4 py-2 text-sm text-blue-100 disabled:opacity-50"
              >
                Hồ sơ
              </button>
              <button
                type="button"
                onClick={fetchMyReservations}
                disabled={!token || loading}
                className="rounded-full bg-blue-500 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                Đơn của tôi
              </button>
            </div>
          </div>
        </header>

        <section className="mt-5 overflow-hidden rounded-3xl border border-white/10 bg-gradient-to-br from-sky-700 via-blue-700 to-indigo-900 p-6 shadow-2xl">
          <div className="grid gap-6 lg:grid-cols-5">
            <div className="space-y-4 lg:col-span-3">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-blue-200">Kỳ nghỉ hoàn hảo</p>
              <h2 className="text-3xl font-bold leading-tight text-white sm:text-4xl">
                Tìm khách sạn và phòng phù hợp theo phong cách đặt phòng hiện đại
              </h2>
              <p className="max-w-2xl text-sm text-blue-100/90">
                Giao diện lấy cảm hứng từ website booking hotel: nổi bật ô tìm kiếm, thẻ khách sạn và luồng đặt phòng nhanh
                từ API hiện có của hệ thống.
              </p>

              <div className="grid gap-3 rounded-2xl bg-slate-950/25 p-4 backdrop-blur sm:grid-cols-2 xl:grid-cols-4">
                <input
                  className="rounded-xl border border-white/20 bg-white/10 px-3 py-2 text-sm text-white placeholder:text-blue-100/70"
                  placeholder="Điểm đến"
                  value={bookingSearch.keyword}
                  onChange={(event) => setBookingSearch((prev) => ({ ...prev, keyword: event.target.value }))}
                />
                <input
                  type="date"
                  className="rounded-xl border border-white/20 bg-white/10 px-3 py-2 text-sm text-white"
                  value={bookingSearch.checkInDate}
                  onChange={(event) => setBookingSearch((prev) => ({ ...prev, checkInDate: event.target.value }))}
                />
                <input
                  type="date"
                  className="rounded-xl border border-white/20 bg-white/10 px-3 py-2 text-sm text-white"
                  value={bookingSearch.checkOutDate}
                  onChange={(event) => setBookingSearch((prev) => ({ ...prev, checkOutDate: event.target.value }))}
                />
                <input
                  type="number"
                  min={1}
                  className="rounded-xl border border-white/20 bg-white/10 px-3 py-2 text-sm text-white"
                  value={bookingSearch.adults}
                  onChange={(event) => setBookingSearch((prev) => ({ ...prev, adults: Number(event.target.value) }))}
                  placeholder="Người lớn"
                />
                <input
                  type="number"
                  min={0}
                  className="rounded-xl border border-white/20 bg-white/10 px-3 py-2 text-sm text-white"
                  value={bookingSearch.children}
                  onChange={(event) => setBookingSearch((prev) => ({ ...prev, children: Number(event.target.value) }))}
                  placeholder="Trẻ em"
                />
                <input
                  type="number"
                  min={1}
                  className="rounded-xl border border-white/20 bg-white/10 px-3 py-2 text-sm text-white"
                  value={bookingSearch.rooms}
                  onChange={(event) => setBookingSearch((prev) => ({ ...prev, rooms: Number(event.target.value) }))}
                  placeholder="Số phòng"
                />
                <button
                  type="button"
                  onClick={searchHotels}
                  disabled={loading}
                  className="rounded-xl bg-white px-3 py-2 text-sm font-semibold text-slate-900 disabled:opacity-50"
                >
                  Tìm khách sạn
                </button>
                <button
                  type="button"
                  onClick={checkAvailability}
                  disabled={loading}
                  className="rounded-xl border border-white/35 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"
                >
                  Kiểm tra phòng trống
                </button>
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
              {stats.map((item) => (
                <article key={item.label} className="rounded-2xl border border-white/15 bg-white/10 p-4 backdrop-blur">
                  <p className="text-sm text-blue-100">{item.label}</p>
                  <p className="mt-1 text-3xl font-bold text-white">{item.value}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        {message ? <p className="mt-4 rounded-xl bg-emerald-500/15 px-4 py-3 text-sm text-emerald-300">{message}</p> : null}
        {error ? <p className="mt-4 rounded-xl bg-red-500/15 px-4 py-3 text-sm text-red-300">{error}</p> : null}

        <section className="mt-6">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-xl font-semibold text-white">Khách sạn đề xuất</h3>
            <p className="text-xs text-slate-400">Dữ liệu từ POST /hotels/search</p>
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {hotels.map((hotel, index) => (
              <article
                key={`${hotel.hotelId ?? hotel.id ?? index}`}
                className="overflow-hidden rounded-2xl border border-white/10 bg-slate-900 shadow-lg"
              >
                <div className="h-36 bg-gradient-to-r from-blue-500/40 to-indigo-500/30" />
                <div className="space-y-2 p-4">
                  <p className="line-clamp-1 text-lg font-semibold text-white">{hotel.name || hotel.hotelName || 'Hotel'}</p>
                  <p className="line-clamp-1 text-sm text-slate-300">{hotel.address || 'Chưa có địa chỉ'}</p>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-yellow-300">⭐ {hotel.starRating ?? '-'}</span>
                    <span className="text-slate-300">ID: {hotel.hotelId ?? hotel.id ?? '-'}</span>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className="mt-8 grid gap-6 xl:grid-cols-2">
          <article className="rounded-2xl border border-white/10 bg-slate-900 p-4">
            <h3 className="text-lg font-semibold text-white">Phòng còn trống</h3>
            <p className="mt-1 text-xs text-slate-400">Dữ liệu từ POST /reservations/availability</p>
            <div className="mt-3 grid gap-3 sm:grid-cols-2">
              <input
                type="number"
                className="rounded-xl border border-white/20 bg-slate-950 px-3 py-2 text-sm"
                value={availability.hotelId}
                onChange={(event) => setAvailability((prev) => ({ ...prev, hotelId: Number(event.target.value) }))}
                placeholder="Hotel ID"
              />
              <input
                type="date"
                className="rounded-xl border border-white/20 bg-slate-950 px-3 py-2 text-sm"
                value={availability.checkInDate}
                onChange={(event) => setAvailability((prev) => ({ ...prev, checkInDate: event.target.value }))}
              />
              <input
                type="date"
                className="rounded-xl border border-white/20 bg-slate-950 px-3 py-2 text-sm"
                value={availability.checkOutDate}
                onChange={(event) => setAvailability((prev) => ({ ...prev, checkOutDate: event.target.value }))}
              />
              <button
                type="button"
                onClick={checkAvailability}
                disabled={loading}
                className="rounded-xl bg-blue-500 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                Làm mới phòng trống
              </button>
            </div>

            <div className="mt-4 space-y-2">
              {availableRooms.map((room, index) => (
                <div
                  key={`${room.roomId ?? room.id ?? index}`}
                  className="flex items-center justify-between rounded-xl border border-white/10 bg-slate-950 px-3 py-2 text-sm"
                >
                  <span>{room.roomNumber || room.code || `Room #${index + 1}`}</span>
                  <span className="text-slate-400">{room.roomTypeName || room.roomTypeId || 'Standard'}</span>
                </div>
              ))}
            </div>
          </article>

          <article className="rounded-2xl border border-white/10 bg-slate-900 p-4">
            <h3 className="text-lg font-semibold text-white">Đặt phòng nhanh</h3>
            <p className="mt-1 text-xs text-slate-400">Tạo booking từ POST /reservations</p>
            <textarea
              rows={10}
              value={createReservationPayload}
              onChange={(event) => setCreateReservationPayload(event.target.value)}
              className="mt-3 w-full rounded-xl border border-white/15 bg-slate-950 p-3 font-mono text-xs"
            />
            <button
              type="button"
              onClick={createReservation}
              disabled={loading || !token}
              className="mt-3 rounded-xl bg-emerald-500 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
            >
              Tạo đặt phòng
            </button>
            <div className="mt-3">
              <JsonBlock value={createdReservation} />
            </div>
          </article>
        </section>

        <section className="mt-8 grid gap-6 xl:grid-cols-2">
          <article className="rounded-2xl border border-white/10 bg-slate-900 p-4">
            <h3 className="text-lg font-semibold text-white">Tài khoản vận hành</h3>
            <form
              className="mt-3 space-y-3"
              onSubmit={(event) => {
                event.preventDefault()
                login()
              }}
            >
              <input
                className="w-full rounded-xl border border-white/20 bg-slate-950 px-3 py-2 text-sm"
                value={loginPayload.usernameOrEmail}
                onChange={(event) => setLoginPayload((prev) => ({ ...prev, usernameOrEmail: event.target.value }))}
                placeholder="Username hoặc Email"
              />
              <input
                type="password"
                className="w-full rounded-xl border border-white/20 bg-slate-950 px-3 py-2 text-sm"
                value={loginPayload.password}
                onChange={(event) => setLoginPayload((prev) => ({ ...prev, password: event.target.value }))}
                placeholder="Mật khẩu"
              />
              <textarea
                rows={3}
                value={token}
                onChange={(event) => {
                  setToken(event.target.value)
                  localStorage.setItem(TOKEN_KEY, event.target.value)
                }}
                className="w-full rounded-xl border border-white/20 bg-slate-950 p-2 font-mono text-xs"
                placeholder="******"
              />
              <div className="flex flex-wrap gap-2">
                <button
                  type="submit"
                  disabled={loading}
                  className="rounded-xl bg-blue-500 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                >
                  Đăng nhập
                </button>
                <button
                  type="button"
                  onClick={fetchUsers}
                  disabled={loading || !token}
                  className="rounded-xl border border-white/30 px-4 py-2 text-sm text-slate-100 disabled:opacity-50"
                >
                  Tải users
                </button>
              </div>
            </form>
          </article>

          <article className="rounded-2xl border border-white/10 bg-slate-900 p-4">
            <h3 className="text-lg font-semibold text-white">Người dùng & hồ sơ</h3>
            <div className="mt-3 grid gap-2 sm:grid-cols-3">
              <input
                className="rounded-xl border border-white/20 bg-slate-950 px-3 py-2 text-sm"
                placeholder="Keyword"
                value={userFilter.keyword}
                onChange={(event) => setUserFilter((prev) => ({ ...prev, keyword: event.target.value }))}
              />
              <select
                className="rounded-xl border border-white/20 bg-slate-950 px-3 py-2 text-sm"
                value={userFilter.status}
                onChange={(event) => setUserFilter((prev) => ({ ...prev, status: event.target.value }))}
              >
                {USER_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
              <button
                type="button"
                onClick={fetchUsers}
                disabled={loading || !token}
                className="rounded-xl bg-indigo-500 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                Lọc users
              </button>
            </div>

            <div className="mt-4 space-y-2">
              {users.slice(0, 5).map((user, index) => (
                <div key={`${user.userId ?? index}`} className="rounded-xl border border-white/10 bg-slate-950 px-3 py-2 text-sm">
                  <p className="font-semibold text-white">{user.fullName || user.username || `User #${index + 1}`}</p>
                  <p className="text-xs text-slate-400">{user.email || 'N/A'}</p>
                </div>
              ))}
            </div>
            <div className="mt-3 grid gap-3">
              <div>
                <p className="mb-1 text-xs text-slate-400">Profile</p>
                <JsonBlock value={profile} />
              </div>
              <div>
                <p className="mb-1 text-xs text-slate-400">My reservations</p>
                <JsonBlock value={myReservations.map((item) => ({
                  reservationId: item.reservationId,
                  status: item.status,
                  checkInDate: formatDate(item.checkInDate),
                  checkOutDate: formatDate(item.checkOutDate),
                }))} />
              </div>
            </div>
          </article>
        </section>
      </div>
    </main>
  )
}
