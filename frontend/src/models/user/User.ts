import Course from '@/models/user/Course';

export default class User {
  id!: number;
  name!: string;
  username!: string;
  email!: string;
  role!: string;
  admin!: boolean;
  creationDate!: string;
  lastAccess!: string;

  constructor(jsonObj?: User) {
    if (jsonObj) {
      this.id = jsonObj.id;
      this.name = jsonObj.name;
      this.username = jsonObj.username;
      this.role = jsonObj.role;
      this.admin = jsonObj.admin;
      this.creationDate = jsonObj.creationDate;
      this.lastAccess = jsonObj.lastAccess;
    }
  }
}