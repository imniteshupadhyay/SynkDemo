package com.playmotech.api.core.utils;

import java.util.EnumSet;

public class EnumUtil {
	public enum RoleType {
		ADMIN("ADMIN"), SUPER_ADMIN("SUPER_ADMIN"), COACH("COACH"), PLAYER("PLAYER"), ACADEMY_OWNER("ACADEMY_OWNER"),
		CLUSTER_HEAD("CLUSTER_HEAD"), PROGRAM_MANAGER("PROGRAM_MANAGER");

		private final String role;

		RoleType(String role) {
			this.role = role;
		}

		public String getRole() {
			return role;
		}

		private static final EnumSet<RoleType> NON_EDITABLE_ROLES = EnumSet.of(ADMIN, SUPER_ADMIN);

		/**
		 * Checks if a role is non-editable.
		 *
		 * @param roleName The name of the role to check.
		 * @return {@code true} if the role is non-editable, {@code false} otherwise.
		 */
		public static boolean isNonEditable(String roleName) {
			for (RoleType role : NON_EDITABLE_ROLES) {
				if (role.name().equalsIgnoreCase(roleName) || role.getRole().equalsIgnoreCase(roleName)) {
					return true;
				}
			}
			return false;
		}
	}
}
