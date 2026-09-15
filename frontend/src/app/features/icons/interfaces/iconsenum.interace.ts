export interface IconOption {
  value: string; // CSS class or identifier
  labelKey: string; // Translation key of the human-readable name (`icons.*`)
  preview: string; // Character or class used to render the preview
}

export const FINANCIAL_ICONS: IconOption[] = [
  { value: 'fa-wallet', labelKey: 'icons.wallet', preview: 'fa-wallet' },
  {
    value: 'fa-building-columns',
    labelKey: 'icons.bank',
    preview: 'fa-building-columns',
  },
  { value: 'fa-credit-card', labelKey: 'icons.creditCard', preview: 'fa-credit-card' },
  { value: 'fa-money-bill-wave', labelKey: 'icons.cash', preview: 'fa-money-bill-wave' },
  { value: 'fa-piggy-bank', labelKey: 'icons.savings', preview: 'fa-piggy-bank' },
  { value: 'fa-coins', labelKey: 'icons.cryptoCoins', preview: 'fa-coins' },
  { value: 'fa-chart-line', labelKey: 'icons.investments', preview: 'fa-chart-line' },
  { value: 'fa-briefcase', labelKey: 'icons.business', preview: 'fa-briefcase' },
];

export const CATEGORY_ICONS: IconOption[] = [
  // 1. Food & Drinks
  { value: 'fa-utensils', labelKey: 'icons.restaurants', preview: 'fa-utensils' },
  { value: 'fa-burger', labelKey: 'icons.fastFood', preview: 'fa-burger' },
  {
    value: 'fa-basket-shopping',
    labelKey: 'icons.supermarket',
    preview: 'fa-basket-shopping',
  },
  { value: 'fa-coffee', labelKey: 'icons.cafe', preview: 'fa-coffee' },
  {
    value: 'fa-glass-martini-alt',
    labelKey: 'icons.barNightlife',
    preview: 'fa-glass-martini-alt',
  },

  // 2. Transportation & Vehicles
  { value: 'fa-car', labelKey: 'icons.carExpenses', preview: 'fa-car' },
  { value: 'fa-gas-pump', labelKey: 'icons.fuel', preview: 'fa-gas-pump' },
  { value: 'fa-bus', labelKey: 'icons.publicTransport', preview: 'fa-bus' },
  { value: 'fa-plane', labelKey: 'icons.travelFlights', preview: 'fa-plane' },
  { value: 'fa-motorcycle', labelKey: 'icons.motorcycle', preview: 'fa-motorcycle' },

  // 3. Housing & Utilities
  { value: 'fa-house', labelKey: 'icons.rentHome', preview: 'fa-house' },
  { value: 'fa-bolt', labelKey: 'icons.electricityUtilities', preview: 'fa-bolt' },
  { value: 'fa-faucet', labelKey: 'icons.waterServices', preview: 'fa-faucet' },
  { value: 'fa-wifi', labelKey: 'icons.internetTelecom', preview: 'fa-wifi' },
  { value: 'fa-couch', labelKey: 'icons.furnitureDecor', preview: 'fa-couch' },

  // 4. Entertainment & Leisure
  { value: 'fa-film', labelKey: 'icons.cinemaStreaming', preview: 'fa-film' },
  { value: 'fa-gamepad', labelKey: 'icons.gaming', preview: 'fa-gamepad' },
  { value: 'fa-music', labelKey: 'icons.musicConcerts', preview: 'fa-music' },
  { value: 'fa-football', labelKey: 'icons.sportsFootball', preview: 'fa-football' },
  { value: 'fa-ticket', labelKey: 'icons.eventsTickets', preview: 'fa-ticket' },

  // 5. Shopping & Personal Care
  { value: 'fa-shirt', labelKey: 'icons.clothingApparel', preview: 'fa-shirt' },
  { value: 'fa-bag-shopping', labelKey: 'icons.shopping', preview: 'fa-bag-shopping' },
  { value: 'fa-spa', labelKey: 'icons.personalCareBeauty', preview: 'fa-spa' },
  { value: 'fa-gift', labelKey: 'icons.giftsDonations', preview: 'fa-gift' },
  { value: 'fa-dumbbell', labelKey: 'icons.gymFitness', preview: 'fa-dumbbell' },

  // 6. Health & Medical
  {
    value: 'fa-heart-pulse',
    labelKey: 'icons.healthMedical',
    preview: 'fa-heart-pulse',
  },
  { value: 'fa-pills', labelKey: 'icons.pharmacyMedicine', preview: 'fa-pills' },
  { value: 'fa-user-doctor', labelKey: 'icons.doctorVisit', preview: 'fa-user-doctor' },

  // 7. Education & Work
  { value: 'fa-book', labelKey: 'icons.booksEducation', preview: 'fa-book' },
  {
    value: 'fa-graduation-cap',
    labelKey: 'icons.tuitionCourses',
    preview: 'fa-graduation-cap',
  },
  {
    value: 'fa-laptop',
    labelKey: 'icons.softwareSubscriptions',
    preview: 'fa-laptop',
  },

  // 8. Financial & Others
  {
    value: 'fa-hand-holding-dollar',
    labelKey: 'icons.loansDebts',
    preview: 'fa-hand-holding-dollar',
  },
  {
    value: 'fa-money-bill-trend-up',
    labelKey: 'icons.salaryIncome',
    preview: 'fa-money-bill-trend-up',
  },
  { value: 'fa-tags', labelKey: 'icons.othersGeneral', preview: 'fa-tags' },
  {
    value: 'fa-vr-cardboard',
    labelKey: 'icons.virtualReality',
    preview: 'fa-vr-cardboard',
  },
  { value: 'fa-microchip', labelKey: 'icons.techHardware', preview: 'fa-microchip' },
  { value: 'fa-code', labelKey: 'icons.programming', preview: 'fa-code' },
  { value: 'fa-palette', labelKey: 'icons.artsDesign', preview: 'fa-palette' },
  { value: 'fa-camera', labelKey: 'icons.photography', preview: 'fa-camera' },
  { value: 'fa-bicycle', labelKey: 'icons.cycling', preview: 'fa-bicycle' },
  { value: 'fa-swimmer', labelKey: 'icons.swimming', preview: 'fa-swimmer' },
  { value: 'fa-dog', labelKey: 'icons.pets', preview: 'fa-dog' },
  { value: 'fa-baby', labelKey: 'icons.childcare', preview: 'fa-baby' },
  { value: 'fa-tree', labelKey: 'icons.gardening', preview: 'fa-tree' },
  { value: 'fa-hammer', labelKey: 'icons.diyRepairs', preview: 'fa-hammer' },
  {
    value: 'fa-scale-balanced',
    labelKey: 'icons.legalTaxes',
    preview: 'fa-scale-balanced',
  },
  {
    value: 'fa-building-columns',
    labelKey: 'icons.government',
    preview: 'fa-building-columns',
  },
  {
    value: 'fa-shield-halved',
    labelKey: 'icons.insurance',
    preview: 'fa-shield-halved',
  },
  { value: 'fa-gift', labelKey: 'icons.donations', preview: 'fa-gift' },
  {
    value: 'fa-people-group',
    labelKey: 'icons.socialFamily',
    preview: 'fa-people-group',
  },
  { value: 'fa-phone', labelKey: 'icons.mobilePlan', preview: 'fa-phone' },
  { value: 'fa-tv', labelKey: 'icons.electronics', preview: 'fa-tv' },
  {
    value: 'fa-calendar-days',
    labelKey: 'icons.subscriptions',
    preview: 'fa-calendar-days',
  },

  {
    value: 'fa-chart-line',
    labelKey: 'icons.stocksInvestments',
    preview: 'fa-chart-line',
  },
  { value: 'fa-chart-pie', labelKey: 'icons.portfolio', preview: 'fa-chart-pie' },
  { value: 'fa-bitcoin', labelKey: 'icons.crypto', preview: 'fa-bitcoin' },
  { value: 'fa-landmark', labelKey: 'icons.bondsBank', preview: 'fa-landmark' },
  { value: 'fa-arrow-trend-up', labelKey: 'icons.growth', preview: 'fa-arrow-trend-up' },
  { value: 'fa-volleyball', labelKey: 'icons.volleyball', preview: 'fa-volleyball' },
  { value: 'fa-basketball', labelKey: 'icons.basketball', preview: 'fa-basketball' },
  {
    value: 'fa-table-tennis-paddle-ball',
    labelKey: 'icons.tennis',
    preview: 'fa-table-tennis-paddle-ball',
  },
  {
    value: 'fa-person-running',
    labelKey: 'icons.running',
    preview: 'fa-person-running',
  },
  { value: 'fa-trophy', labelKey: 'icons.awardsCompetitions', preview: 'fa-trophy' },
];
