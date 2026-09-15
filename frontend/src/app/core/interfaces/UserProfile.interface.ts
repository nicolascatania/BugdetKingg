/** The signed-in user's own profile, as returned by GET /me and PATCH /me. */
export interface UserProfile {
  email: string;
  name: string;
  lastName: string;
  picture: string | null;
  /** ISO 3166-1 alpha-2 country code, or null until the user picks one in Settings. */
  country: string | null;
}

/** Body of PATCH /me. Only `country` is editable today; `null` clears it. */
export interface UpdateProfileRequest {
  country: string | null;
}
