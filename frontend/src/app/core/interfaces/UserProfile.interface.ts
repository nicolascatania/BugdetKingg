/** The signed-in user's own profile, as returned by GET /me. */
export interface UserProfile {
  email: string;
  name: string;
  lastName: string;
  picture: string | null;
}
