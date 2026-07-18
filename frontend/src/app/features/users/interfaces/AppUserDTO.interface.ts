export interface AppUserDTO {
  id: string;
  name: string;
  lastName: string;
  email: string;
  enabled: boolean;
}


export interface AppUserForListDTO {
    id: string;
    name: string;
    lastName: string;
    email: string;
    enabled: boolean;
    roles: string[];
}