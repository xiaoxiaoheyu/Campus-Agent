const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { join } = require('node:path')
const test = require('node:test')

const FRONTEND_ROOT = join(__dirname, '..', '..')

test('home starts with the hero and does not render a title bar', () => {
  const source = readFileSync(join(__dirname, 'index.vue'), 'utf8')
  const pages = JSON.parse(readFileSync(join(FRONTEND_ROOT, 'pages.json'), 'utf8'))
  const homePage = pages.pages.find((page) => page.path === 'pages/index/index')

  assert.ok(homePage)
  assert.equal(homePage.style.navigationStyle, 'custom')
  assert.equal(homePage.style.navigationBarTitleText, '')
  assert.doesNotMatch(source, /common-page-header/i)
  assert.match(source, /<view class="home-page">\s*<view class="hero-shell">/)
})

test('home quick entry uses the approved five-entry design and keeps every destination', () => {
  const source = readFileSync(join(__dirname, 'index.vue'), 'utf8')

  const icons = ['activity', 'food', 'trade', 'forum', 'promotion']
  for (const icon of icons) {
    assert.match(source, new RegExp(`/static/index/quick-entry/${icon}\\.png`))
  }
  assert.equal((source.match(/class="home-quick-entry__icon"/g) || []).length, 5)
  assert.doesNotMatch(source, /home-quick-entry\.jpg/)
  assert.doesNotMatch(source, /home-quick-entry__design/)
  assert.match(source, /\.home-quick-entry\s*\{[^}]*grid-template-columns:\s*repeat\(5,[^}]*gap:\s*12rpx/s)
  assert.match(source, /\.home-quick-entry__item\s*\{[^}]*height:\s*142rpx[^}]*border-radius:\s*24rpx/s)
  assert.match(source, /aria-label="活动"/)
  assert.match(source, /aria-label="美食"/)
  assert.match(source, /aria-label="交易"/)
  assert.match(source, /aria-label="论坛"/)
  assert.match(source, /aria-label="优惠"/)
  assert.match(source, /\/subpackage_community\/communityActivity\/communityActivity/)
  assert.match(source, /\/subpackage_facility\/restaurantDetail\/restaurantDetail\?id=3/)
  assert.match(source, /\/subpackage_lostfound\/marketplaceHome\/marketplaceHome/)
  assert.match(source, /\/subpackage_forum\/forumList\/forumList/)
  assert.match(source, /\/subpackage_promotion\/promotion\/promotion/)
})

test('home carousel uses the four supplied posters', () => {
  const source = readFileSync(join(__dirname, 'index.vue'), 'utf8')
  const posters = [
    'hero-water-conservation.jpg',
    'hero-food-day.jpg',
    'hero-martyrs-day.jpg',
    'hero-campus-building.jpg'
  ]

  for (const poster of posters) {
    assert.match(source, new RegExp(`/static/index/${poster.replace('.', '\\.')}`))
  }
  assert.equal((source.match(/image: '\/static\/index\/hero-/g) || []).length, 4)
})

test('background unread refresh degrades without a user-facing error toast', () => {
  const store = readFileSync(join(FRONTEND_ROOT, 'utils', 'messageStore.js'), 'utf8')
  assert.match(store, /getAppMessageUnreadCount\(\{ showError: false \}\)/)
  assert.match(store, /if \(!getToken\(\)\) return getMessageState\(\)/)
})
