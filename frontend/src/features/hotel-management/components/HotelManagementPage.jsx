import { useMemo, useState } from 'react'
import { API_BASE_URL } from '../../../shared/config/env'
import { requestApi } from '../../../shared/lib/httpClient'
import { JsonBlock } from '../../../shared/ui/JsonBlock'

const TOKEN_KEY = 'hotel_service_api_token'

const ROOM_STATUSES = ['AVAILABLE', 'HELD', 'OCCUPIED', 'MAINTENANCE', 'REMOVED']
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

export function HotelManagementPage() {
  const [activeTab, setActiveTab] = useState('dashboard')
  const [token, setToken] = useState(getStoredToken)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const [loginPayload, setLoginPayload] = useState({
    usernameOrEmail: 'guest@example.com',
    password: '123456',
  })

  const [hotelSearch, setHotelSearch] = useState({ keyword: '', page: 0, size: 10 })
  const [roomFilter, setRoomFilter] = useState({ hotelId: 1, status: 'AVAILABLE', page: 0, size: 10 })
  const [userFilter, setUserFilter] = useState({ keyword: '', status: 'ACTIVE', page: 0, size: 10 })

  const [hotels, setHotels] = useState([])
  const [rooms, setRooms] = useState([])
  const [users, setUsers] = useState([])
  const [myReservations, setMyReservations] = useState([])
  const [profile, setProfile] = useState(null)

  const [availability, setAvailability] = useState({
    hotelId: 1,
    checkInDate: '2026-04-01',
    checkOutDate: '2026-04-02',
    adults: 1,
    children: 0,
  })
  const [availableRooms, setAvailableRooms] = useState([])

  const [createReservationPayload, setCreateReservationPayload] = useState(`{
  "hotelId": 1,
  "checkInDate": "2026-04-01",
  "checkOutDate": "2026-04-03",
  "adultCount": 2,
  "childCount": 0,
  "specialRequests": "Non-smoking",
  "rooms": [{ "roomTypeId": 1 }]
}`)
  const [createdReservation, setCreatedReservation] = useState(null)

  const stats = useMemo(
    () => [
      { label: 'Khách sạn', value: hotels.length },
      { label: 'Phòng', value: rooms.length },
      { label: 'Người dùng', value: users.length },
      { label: 'Đặt phòng của tôi', value: myReservations.length },
    ],
    [hotels.length, rooms.length, users.length, myReservations.length],
  )

  const callApi = async (executor, successMessage) => {
    setLoading(true)
    setError('')
    setMessage('')
    try {
      await executor()
      if (successMessage) {
        setMessage(successMessage)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra')
    } finally {
      setLoading(false)
    }
  }

  const login = () =>
    callApi(async () => {
      const result = await requestApi({ method: 'POST', path: '/auth/login', body: loginPayload })
      if (!result.ok) {
        throw new Error(`Đăng nhập thất bại (HTTP ${result.status})`)
      }

      const data = pickData(result.data)
      const accessToken = data?.accessToken
      setProfile(data?.user ?? null)

      if (accessToken) {
        setToken(accessToken)
        localStorage.setItem(TOKEN_KEY, accessToken)
      }
    }, 'Đăng nhập thành công')

  const fetchMyProfile = () =>
    callApi(async () => {
      const result = await requestApi({ method: 'GET', path: '/users/me', token })
      if (!result.ok) {
        throw new Error(`Lấy profile thất bại (HTTP ${result.status})`)
      }
      setProfile(pickData(result.data))
    }, 'Đã tải profile')

  const searchHotels = () =>
    callApi(async () => {
      const result = await requestApi({ method: 'POST', path: '/hotels/search', body: hotelSearch })
      if (!result.ok) {
        throw new Error(`Tìm khách sạn thất bại (HTTP ${result.status})`)
      }
      setHotels(normalizeList(pickData(result.data)))
    }, 'Đã tải danh sách khách sạn')

  const fetchRooms = () =>
    callApi(async () => {
      const result = await requestApi({
        method: 'GET',
        path: '/rooms',
        query: roomFilter,
      })
      if (!result.ok) {
        throw new Error(`Lấy phòng thất bại (HTTP ${result.status})`)
      }
      setRooms(normalizeList(pickData(result.data)))
    }, 'Đã tải danh sách phòng')

  const fetchUsers = () =>
    callApi(async () => {
      const result = await requestApi({ method: 'GET', path: '/users', token, query: userFilter })
      if (!result.ok) {
        throw new Error(`Lấy users thất bại (HTTP ${result.status})`)
      }
      setUsers(normalizeList(pickData(result.data)))
    }, 'Đã tải danh sách người dùng')

  const fetchMyReservations = () =>
    callApi(async () => {
      const result = await requestApi({ method: 'GET', path: '/reservations/me', token })
      if (!result.ok) {
        throw new Error(`Lấy reservations thất bại (HTTP ${result.status})`)
      }
      setMyReservations(normalizeList(pickData(result.data)))
    }, 'Đã tải danh sách đặt phòng')

  const checkAvailability = () =>
    callApi(async () => {
      const result = await requestApi({
        method: 'POST',
        path: '/reservations/availability',
        body: availability,
      })
      if (!result.ok) {
        throw new Error(`Kiểm tra phòng trống thất bại (HTTP ${result.status})`)
      }
      setAvailableRooms(normalizeList(pickData(result.data)))
    }, 'Đã tải phòng trống')

  const createReservation = () =>
    callApi(async () => {
      let payload
      try {
        payload = JSON.parse(createReservationPayload)
      } catch {
        throw new Error('JSON tạo reservation không hợp lệ')
      }

      const result = await requestApi({ method: 'POST', path: '/reservations', token, body: payload })
      if (!result.ok) {
        throw new Error(`Tạo reservation thất bại (HTTP ${result.status})`)
      }
      setCreatedReservation(pickData(result.data))
    }, 'Tạo reservation thành công')

  const tabs = [
    { key: 'dashboard', label: 'Dashboard' },
    { key: 'hotels', label: 'Khách sạn' },
    { key: 'rooms', label: 'Phòng' },
    { key: 'reservations', label: 'Đặt phòng' },
    { key: 'users', label: 'Người dùng' },
  ]

  return (
    <main className="min-h-screen bg-slate-100 p-4 text-slate-900">
      <div className="mx-auto max-w-7xl space-y-4">
        <header className="rounded-xl bg-white p-5 shadow-sm">
          <h1 className="text-2xl font-bold">UI Quản lý khách sạn</h1>
          <p className="mt-1 text-sm text-slate-600">Vận hành nghiệp vụ khách sạn dựa trên API backend hiện có.</p>
          <p className="mt-1 text-xs text-slate-500">
            API base URL: <span className="font-mono">{API_BASE_URL}</span>
          </p>

          <form
            className="mt-4"
            onSubmit={(event) => {
              event.preventDefault()
              login()
            }}
          >
            <div className="grid gap-3 lg:grid-cols-2">
              <label className="text-xs text-slate-600">
                Username/Email
                <input
                  className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                  value={loginPayload.usernameOrEmail}
                  onChange={(event) => setLoginPayload((prev) => ({ ...prev, usernameOrEmail: event.target.value }))}
                />
              </label>
              <label className="text-xs text-slate-600">
                Password
                <input
                  type="password"
                  className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                  value={loginPayload.password}
                  onChange={(event) => setLoginPayload((prev) => ({ ...prev, password: event.target.value }))}
                />
              </label>
            </div>

            <div className="mt-3 flex flex-wrap gap-2">
              <button
                type="submit"
                disabled={loading}
                className="rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
              >
                Đăng nhập
              </button>
              <button
                type="button"
                disabled={loading || !token}
                onClick={fetchMyProfile}
                className="rounded-md bg-slate-800 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-900 disabled:opacity-50"
              >
                Tải profile
              </button>
            </div>
          </form>

          <label className="mt-3 block text-xs text-slate-600">
            Bearer token
            <textarea
              rows={3}
              value={token}
              onChange={(event) => {
                setToken(event.target.value)
                localStorage.setItem(TOKEN_KEY, event.target.value)
              }}
              className="mt-1 w-full rounded-md border border-slate-300 p-2 font-mono text-xs"
              placeholder="Dán access token ở đây"
            />
          </label>

          {message ? <p className="mt-2 text-sm text-green-600">{message}</p> : null}
          {error ? <p className="mt-2 text-sm text-red-600">{error}</p> : null}
        </header>

        <div className="rounded-xl bg-white p-3 shadow-sm">
          <div className="flex flex-wrap gap-2">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                type="button"
                onClick={() => setActiveTab(tab.key)}
                className={`rounded-md px-3 py-2 text-sm font-medium ${
                  activeTab === tab.key ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {activeTab === 'dashboard' ? (
          <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            {stats.map((item) => (
              <article key={item.label} className="rounded-xl bg-white p-4 shadow-sm">
                <p className="text-sm text-slate-500">{item.label}</p>
                <p className="mt-2 text-2xl font-bold text-slate-900">{item.value}</p>
              </article>
            ))}
          </section>
        ) : null}

        {activeTab === 'hotels' ? (
          <section className="space-y-3 rounded-xl bg-white p-4 shadow-sm">
            <h2 className="text-lg font-semibold">Quản lý khách sạn</h2>
            <div className="grid gap-3 md:grid-cols-3">
              <input
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                placeholder="Từ khóa"
                value={hotelSearch.keyword}
                onChange={(event) => setHotelSearch((prev) => ({ ...prev, keyword: event.target.value }))}
              />
              <input
                type="number"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={hotelSearch.page}
                onChange={(event) => setHotelSearch((prev) => ({ ...prev, page: Number(event.target.value) }))}
              />
              <button
                type="button"
                onClick={searchHotels}
                disabled={loading}
                className="rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                Tìm khách sạn
              </button>
            </div>

            <div className="grid gap-3 lg:grid-cols-2">
              {hotels.map((hotel, index) => (
                <article key={`${hotel.hotelId ?? hotel.id ?? index}`} className="rounded-lg border border-slate-200 p-3">
                  <p className="font-semibold">{hotel.name || hotel.hotelName || `Hotel #${index + 1}`}</p>
                  <p className="text-sm text-slate-600">{hotel.address || 'Không có địa chỉ'}</p>
                  <p className="text-xs text-slate-500">⭐ {hotel.starRating ?? '-'}</p>
                </article>
              ))}
            </div>
          </section>
        ) : null}

        {activeTab === 'rooms' ? (
          <section className="space-y-3 rounded-xl bg-white p-4 shadow-sm">
            <h2 className="text-lg font-semibold">Quản lý phòng</h2>
            <div className="grid gap-3 md:grid-cols-4">
              <input
                type="number"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={roomFilter.hotelId}
                onChange={(event) => setRoomFilter((prev) => ({ ...prev, hotelId: Number(event.target.value) }))}
                placeholder="Hotel ID"
              />
              <select
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={roomFilter.status}
                onChange={(event) => setRoomFilter((prev) => ({ ...prev, status: event.target.value }))}
              >
                {ROOM_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
              <input
                type="number"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={roomFilter.page}
                onChange={(event) => setRoomFilter((prev) => ({ ...prev, page: Number(event.target.value) }))}
                placeholder="Page"
              />
              <button
                type="button"
                onClick={fetchRooms}
                disabled={loading}
                className="rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                Tải danh sách phòng
              </button>
            </div>

            <div className="overflow-auto rounded-lg border border-slate-200">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase text-slate-600">
                  <tr>
                    <th className="px-3 py-2">Room</th>
                    <th className="px-3 py-2">Hotel</th>
                    <th className="px-3 py-2">Type</th>
                    <th className="px-3 py-2">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {rooms.map((room, index) => (
                    <tr key={`${room.roomId ?? room.id ?? index}`} className="border-t border-slate-200">
                      <td className="px-3 py-2">{room.roomNumber || room.code || '-'}</td>
                      <td className="px-3 py-2">{room.hotelName || room.hotelId || '-'}</td>
                      <td className="px-3 py-2">{room.roomTypeName || room.roomTypeId || '-'}</td>
                      <td className="px-3 py-2">{room.status || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        ) : null}

        {activeTab === 'reservations' ? (
          <section className="space-y-3 rounded-xl bg-white p-4 shadow-sm">
            <h2 className="text-lg font-semibold">Quản lý đặt phòng</h2>

            <div className="grid gap-3 md:grid-cols-5">
              <input
                type="number"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={availability.hotelId}
                onChange={(event) => setAvailability((prev) => ({ ...prev, hotelId: Number(event.target.value) }))}
                placeholder="Hotel ID"
              />
              <input
                type="date"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={availability.checkInDate}
                onChange={(event) => setAvailability((prev) => ({ ...prev, checkInDate: event.target.value }))}
              />
              <input
                type="date"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={availability.checkOutDate}
                onChange={(event) => setAvailability((prev) => ({ ...prev, checkOutDate: event.target.value }))}
              />
              <button
                type="button"
                onClick={checkAvailability}
                disabled={loading}
                className="rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                Kiểm tra phòng trống
              </button>
              <button
                type="button"
                onClick={fetchMyReservations}
                disabled={loading || !token}
                className="rounded-md bg-slate-800 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                Đơn đặt của tôi
              </button>
            </div>

            <label className="block text-xs text-slate-600">
              Payload tạo reservation (JSON)
              <textarea
                rows={10}
                value={createReservationPayload}
                onChange={(event) => setCreateReservationPayload(event.target.value)}
                className="mt-1 w-full rounded-md border border-slate-300 p-2 font-mono text-xs"
              />
            </label>

            <button
              type="button"
              onClick={createReservation}
              disabled={loading || !token}
              className="rounded-md bg-green-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
            >
              Tạo đặt phòng
            </button>

            <div className="grid gap-3 xl:grid-cols-3">
              <article>
                <p className="mb-1 text-sm font-semibold">Phòng trống</p>
                <JsonBlock value={availableRooms} />
              </article>
              <article>
                <p className="mb-1 text-sm font-semibold">Đặt phòng của tôi</p>
                <JsonBlock value={myReservations} />
              </article>
              <article>
                <p className="mb-1 text-sm font-semibold">Kết quả tạo reservation</p>
                <JsonBlock value={createdReservation} />
              </article>
            </div>
          </section>
        ) : null}

        {activeTab === 'users' ? (
          <section className="space-y-3 rounded-xl bg-white p-4 shadow-sm">
            <h2 className="text-lg font-semibold">Quản lý người dùng</h2>
            <div className="grid gap-3 md:grid-cols-4">
              <input
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                placeholder="Keyword"
                value={userFilter.keyword}
                onChange={(event) => setUserFilter((prev) => ({ ...prev, keyword: event.target.value }))}
              />
              <select
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={userFilter.status}
                onChange={(event) => setUserFilter((prev) => ({ ...prev, status: event.target.value }))}
              >
                {USER_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {status}
                  </option>
                ))}
              </select>
              <input
                type="number"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={userFilter.page}
                onChange={(event) => setUserFilter((prev) => ({ ...prev, page: Number(event.target.value) }))}
              />
              <button
                type="button"
                onClick={fetchUsers}
                disabled={loading || !token}
                className="rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                Tải danh sách users
              </button>
            </div>

            <div className="overflow-auto rounded-lg border border-slate-200">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase text-slate-600">
                  <tr>
                    <th className="px-3 py-2">ID</th>
                    <th className="px-3 py-2">Tên</th>
                    <th className="px-3 py-2">Email</th>
                    <th className="px-3 py-2">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user, index) => (
                    <tr key={`${user.userId ?? index}`} className="border-t border-slate-200">
                      <td className="px-3 py-2">{user.userId ?? '-'}</td>
                      <td className="px-3 py-2">{user.fullName || user.username || '-'}</td>
                      <td className="px-3 py-2">{user.email || '-'}</td>
                      <td className="px-3 py-2">{user.status || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        ) : null}

        {profile ? (
          <section className="rounded-xl bg-white p-4 shadow-sm">
            <p className="mb-1 text-sm font-semibold">Thông tin người dùng hiện tại</p>
            <JsonBlock value={profile} />
          </section>
        ) : null}
      </div>
    </main>
  )
}
