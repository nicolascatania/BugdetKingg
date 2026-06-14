export interface IconOption {
  value: string; // Clase CSS o identificador
  label: string; // Texto legible
  preview: string; // Carácter o clase para renderizar
}

export const FINANCIAL_ICONS: IconOption[] = [
  { value: 'fa-wallet', label: 'Wallet', preview: 'fa-wallet' },
  {
    value: 'fa-building-columns',
    label: 'Bank',
    preview: 'fa-building-columns',
  },
  { value: 'fa-credit-card', label: 'Credit Card', preview: 'fa-credit-card' },
  { value: 'fa-money-bill-wave', label: 'Cash', preview: 'fa-money-bill-wave' },
  { value: 'fa-piggy-bank', label: 'Savings', preview: 'fa-piggy-bank' },
  { value: 'fa-coins', label: 'Crypto/Coins', preview: 'fa-coins' },
  { value: 'fa-chart-line', label: 'Investments', preview: 'fa-chart-line' },
  { value: 'fa-briefcase', label: 'Business', preview: 'fa-briefcase' },
];

export const CATEGORY_ICONS: IconOption[] = [
  // 1. Food & Drinks
  { value: 'fa-utensils', label: 'Restaurants', preview: 'fa-utensils' },
  { value: 'fa-burger', label: 'Fast Food', preview: 'fa-burger' },
  {
    value: 'fa-basket-shopping',
    label: 'Supermarket',
    preview: 'fa-basket-shopping',
  },
  { value: 'fa-coffee', label: 'Café', preview: 'fa-coffee' },
  {
    value: 'fa-glass-martini-alt',
    label: 'Bar & Nightlife',
    preview: 'fa-glass-martini-alt',
  },

  // 2. Transportation & Vehicles
  { value: 'fa-car', label: 'Car / Expenses', preview: 'fa-car' },
  { value: 'fa-gas-pump', label: 'Fuel', preview: 'fa-gas-pump' },
  { value: 'fa-bus', label: 'Public Transport', preview: 'fa-bus' },
  { value: 'fa-plane', label: 'Travel & Flights', preview: 'fa-plane' },
  { value: 'fa-motorcycle', label: 'Motorcycle', preview: 'fa-motorcycle' },

  // 3. Housing & Utilities
  { value: 'fa-house', label: 'Rent / Home', preview: 'fa-house' },
  { value: 'fa-bolt', label: 'Electricity / Utilities', preview: 'fa-bolt' },
  { value: 'fa-faucet', label: 'Water Services', preview: 'fa-faucet' },
  { value: 'fa-wifi', label: 'Internet / Telecom', preview: 'fa-wifi' },
  { value: 'fa-couch', label: 'Furniture / Decor', preview: 'fa-couch' },

  // 4. Entertainment & Leisure
  { value: 'fa-film', label: 'Cinema & Streaming', preview: 'fa-film' },
  { value: 'fa-gamepad', label: 'Gaming', preview: 'fa-gamepad' },
  { value: 'fa-music', label: 'Music & Concerts', preview: 'fa-music' },
  { value: 'fa-football', label: 'Sports / Football', preview: 'fa-football' },
  { value: 'fa-ticket', label: 'Events / Tickets', preview: 'fa-ticket' },

  // 5. Shopping & Personal Care
  { value: 'fa-shirt', label: 'Clothing & Apparel', preview: 'fa-shirt' },
  { value: 'fa-bag-shopping', label: 'Shopping', preview: 'fa-bag-shopping' },
  { value: 'fa-spa', label: 'Personal Care & Beauty', preview: 'fa-spa' },
  { value: 'fa-gift', label: 'Gifts & Donations', preview: 'fa-gift' },
  { value: 'fa-dumbbell', label: 'Gym & Fitness', preview: 'fa-dumbbell' },

  // 6. Health & Medical
  {
    value: 'fa-heart-pulse',
    label: 'Health & Medical',
    preview: 'fa-heart-pulse',
  },
  { value: 'fa-pills', label: 'Pharmacy / Medicine', preview: 'fa-pills' },
  { value: 'fa-user-doctor', label: 'Doctor Visit', preview: 'fa-user-doctor' },

  // 7. Education & Work
  { value: 'fa-book', label: 'Books & Education', preview: 'fa-book' },
  {
    value: 'fa-graduation-cap',
    label: 'Tuition / Courses',
    preview: 'fa-graduation-cap',
  },
  {
    value: 'fa-laptop',
    label: 'Software / Subscriptions',
    preview: 'fa-laptop',
  },

  // 8. Financial & Others
  {
    value: 'fa-hand-holding-dollar',
    label: 'Loans / Debts',
    preview: 'fa-hand-holding-dollar',
  },
  {
    value: 'fa-money-bill-trend-up',
    label: 'Salary / Income',
    preview: 'fa-money-bill-trend-up',
  },
  { value: 'fa-tags', label: 'Others / General', preview: 'fa-tags' },
  {
    value: 'fa-vr-cardboard',
    label: 'Virtual Reality',
    preview: 'fa-vr-cardboard',
  },
  { value: 'fa-microchip', label: 'Tech & Hardware', preview: 'fa-microchip' },
  { value: 'fa-code', label: 'Programming', preview: 'fa-code' },
  { value: 'fa-palette', label: 'Arts & Design', preview: 'fa-palette' },
  { value: 'fa-camera', label: 'Photography', preview: 'fa-camera' },
  { value: 'fa-bicycle', label: 'Cycling', preview: 'fa-bicycle' },
  { value: 'fa-swimmer', label: 'Swimming', preview: 'fa-swimmer' },
  { value: 'fa-dog', label: 'Pets', preview: 'fa-dog' },
  { value: 'fa-baby', label: 'Childcare', preview: 'fa-baby' },
  { value: 'fa-tree', label: 'Gardening', preview: 'fa-tree' },
  { value: 'fa-hammer', label: 'DIY / Repairs', preview: 'fa-hammer' },
  {
    value: 'fa-scale-balanced',
    label: 'Legal / Taxes',
    preview: 'fa-scale-balanced',
  },
  {
    value: 'fa-building-columns',
    label: 'Government',
    preview: 'fa-building-columns',
  },
  {
    value: 'fa-shield-halved',
    label: 'Insurance',
    preview: 'fa-shield-halved',
  },
  { value: 'fa-gift', label: 'Donations', preview: 'fa-gift' },
  {
    value: 'fa-people-group',
    label: 'Social / Family',
    preview: 'fa-people-group',
  },
  { value: 'fa-phone', label: 'Mobile Plan', preview: 'fa-phone' },
  { value: 'fa-tv', label: 'Electronics', preview: 'fa-tv' },
  {
    value: 'fa-calendar-days',
    label: 'Subscriptions',
    preview: 'fa-calendar-days',
  },

  {
    value: 'fa-chart-line',
    label: 'Stocks / Investments',
    preview: 'fa-chart-line',
  },
  { value: 'fa-chart-pie', label: 'Portfolio', preview: 'fa-chart-pie' },
  { value: 'fa-bitcoin', label: 'Crypto', preview: 'fa-bitcoin' },
  { value: 'fa-landmark', label: 'Bonds / Bank', preview: 'fa-landmark' },
  { value: 'fa-arrow-trend-up', label: 'Growth', preview: 'fa-arrow-trend-up' },
  { value: 'fa-volleyball', label: 'Volleyball', preview: 'fa-volleyball' },
  { value: 'fa-basketball', label: 'Basketball', preview: 'fa-basketball' },
  {
    value: 'fa-table-tennis-paddle-ball',
    label: 'Tennis',
    preview: 'fa-table-tennis-paddle-ball',
  },
  {
    value: 'fa-person-running',
    label: 'Running',
    preview: 'fa-person-running',
  },
  { value: 'fa-trophy', label: 'Awards / Competitions', preview: 'fa-trophy' },
];
