package com.example.reddy.safety;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
        private String userId;
        private String userName;
        private String balance;
        private String phone;

        public User() {
            //this constructor is required
        }

        public User(String userId, String userName, String balance,String phone) {
            this.userId = userId;
            this.userName = userName;
            this.balance = balance;
            this.phone=phone;
        }

        public String getUserPhone() {
            return phone;
        }

        public String getUserName() {
            return userName;
        }

        public String getBalance() {
            return balance;
        }
}
