package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.UserProfile;

@Repository
public interface UserProfileRepo extends JpaRepository<UserProfile, String> {

	Optional<UserProfile> findByUsername(String username);

	Optional<UserProfile> findByUsernameAndPrimaryAccountIsTrue(String username);

	List<UserProfile> findByIdIn(List<String> ids);

	List<UserProfile> findByIdInAndUsername(List<String> ids, String username);

	List<UserProfile> findByIdInAndPhoneNumber(List<String> ids, String phoneNumber);

	List<UserProfile> findByIdInAndUsernameAndPhoneNumber(List<String> ids, String username, String phoneNumber);

	List<UserProfile> findByUserType(UserType userType);

	// @Query("SELECT u FROM UserProfile u WHERE u.displayName ILIKE %:searchTxt% OR
	// u.username ILIKE 91%:searchTxt%")
	// List<UserProfile> searchUserByNameOrUsername(@Param("searchTxt") String
	// searchTxt);
	//
	// @Query("SELECT u FROM UserProfile u WHERE (u.displayName ILIKE %:searchTxt%
	// OR u.username ILIKE 91%:searchTxt%) AND u.userType = :userType")
	// List<UserProfile> searchUserByNameOrUsernameAndUserType(@Param("searchTxt")
	// String searchTxt,
	// @Param("userType") UserType userType);

	@Query("SELECT u FROM UserProfile u WHERE " + "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :searchTxt, '%')) OR "
			+ "u.username LIKE CONCAT('%', :searchTxt)")
	List<UserProfile> searchUserByNameOrUsername(@Param("searchTxt") String searchTxt);

	@Query("SELECT u FROM UserProfile u WHERE (" + "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :searchTxt, '%')) OR "
			+ "u.username LIKE CONCAT('%', :searchTxt)) AND u.userType = :userType")
	List<UserProfile> searchUserByNameOrUsernameAndUserType(@Param("searchTxt") String searchTxt,
			@Param("userType") UserType userType);

	Optional<UserProfile> findByUsernameOrEmailId(String username, String emailId);

	@Query("SELECT u FROM UserProfile u WHERE (u.username = :username OR u.emailId = :emailId) AND u.primaryAccount = true")
	Optional<UserProfile> findByUsernameOrEmailIdAndPrimaryAccountIsTrue(@Param("username") String username,
			@Param("emailId") String emailId);

	Optional<UserProfile> findByPhoneNumberOrEmailId(String phoneNumber, String emailId);

//	Optional<UserProfile> findByPhoneNumber(String phoneNumber);
	@Query("SELECT u FROM UserProfile u WHERE u.phoneNumber = :phoneNumber AND u.primaryAccount = true")
	Optional<UserProfile> findByPhoneNumberAndPrimaryAccountIsTrue(String phoneNumber);

	Optional<UserProfile> findByEmailId(String email);

	Optional<UserProfile> findByEmailIdAndPrimaryAccountIsTrue(String email);

	Optional<UserProfile> findByIdAndInactiveIsFalse(String id);

	Optional<UserProfile> findByEmailIdAndInactiveIsFalse(String email);

	Optional<UserProfile> findByUsernameAndInactiveIsFalse(String username);

	Optional<UserProfile> findByPhoneNumberAndInactiveIsFalse(String phoneNumber);

	boolean existsByUsername(String username);

	boolean existsByUsernameAndInactiveIsFalse(String username);

	@Modifying
	@Query(value = "UPDATE user_profiles SET inactive = false where id = :id", nativeQuery = true)
	Integer toggleInActiveStatus(@Param("id") String id);

	@Query(value = "SELECT * FROM get_users_list(:userId, :userRole, :domainUrl)", nativeQuery = true)
	List<Object[]> findUsersListRoleWise(@Param("userId") String userId, @Param("userRole") String userRole,
			@Param("domainUrl") String domainUrl);

	List<UserProfile> findByPhoneNumberAndInactive(String phoneNumber, boolean status);

	boolean existsByPhoneNumberAndInactive(String phoneNumber, boolean b);

	@Query(value = "SELECT u FROM UserProfile u WHERE u.primaryAccount = true and (u.emailId = :emailId or u.phoneNumber = :phoneNumber or u.username = :username)")
	Optional<UserProfile> findByUsernameOrEmailIdOrPhoneNumberAndPrimaryAccountIsTrue(String emailId,
			String phoneNumber, String username);

	List<UserProfile> findByUsernameAndInactive(String username, boolean status);

	@Query("SELECT u FROM UserProfile u WHERE u.phoneNumber = :phoneNumber")
	List<UserProfile> findByPhoneNumberList(String phoneNumber);

	// Add these methods to your UserProfileRepository interface

	/**
	 * Find users by email ID and inactive status
	 * 
	 * @param emailId  The email ID to search for
	 * @param inactive The inactive status (false for active users)
	 * @return List of UserProfile entities matching the criteria
	 */
	List<UserProfile> findByEmailIdAndInactive(String emailId, boolean inactive);

	/**
	 * Find users by email ID (case insensitive) and inactive status
	 * 
	 * @param emailId  The email ID to search for
	 * @param inactive The inactive status (false for active users)
	 * @return List of UserProfile entities matching the criteria
	 */
	@Query("SELECT u FROM UserProfile u WHERE LOWER(u.emailId) = LOWER(:emailId) AND u.inactive = :inactive")
	List<UserProfile> findByEmailIdIgnoreCaseAndInactive(@Param("emailId") String emailId,
			@Param("inactive") boolean inactive);

	/**
	 * Find users by phone number and display name (case insensitive)
	 * 
	 * @param phoneNumber The phone number to search for
	 * @param displayName The display name to search for
	 * @param inactive    The inactive status (false for active users)
	 * @return List of UserProfile entities matching the criteria
	 */
	@Query("SELECT u FROM UserProfile u WHERE u.phoneNumber = :phoneNumber AND LOWER(u.displayName) = LOWER(:displayName) AND u.inactive = :inactive")
	List<UserProfile> findByPhoneNumberAndDisplayNameIgnoreCaseAndInactive(@Param("phoneNumber") String phoneNumber,
			@Param("displayName") String displayName, @Param("inactive") boolean inactive);

	/**
	 * Check if display name exists for a specific phone number (excluding a
	 * specific user ID)
	 * 
	 * @param phoneNumber   The phone number to check
	 * @param displayName   The display name to check
	 * @param excludeUserId The user ID to exclude from the check
	 * @param inactive      The inactive status (false for active users)
	 * @return True if display name exists for the phone number, false otherwise
	 */
	@Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserProfile u "
			+ "WHERE u.phoneNumber = :phoneNumber AND LOWER(u.displayName) = LOWER(:displayName) "
			+ "AND u.inactive = :inactive AND u.id != :excludeUserId")
	boolean existsByPhoneNumberAndDisplayNameIgnoreCaseAndInactiveAndIdNot(@Param("phoneNumber") String phoneNumber,
			@Param("displayName") String displayName, @Param("inactive") boolean inactive,
			@Param("excludeUserId") String excludeUserId);

	/**
	 * Check if email exists with different phone number (excluding a specific user
	 * ID)
	 * 
	 * @param emailId       The email ID to check
	 * @param phoneNumber   The phone number to exclude
	 * @param excludeUserId The user ID to exclude from the check
	 * @param inactive      The inactive status (false for active users)
	 * @return True if email exists with different phone number, false otherwise
	 */
	@Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserProfile u "
			+ "WHERE LOWER(u.emailId) = LOWER(:emailId) AND u.phoneNumber != :phoneNumber "
			+ "AND u.inactive = :inactive AND u.id != :excludeUserId")
	boolean existsByEmailIdIgnoreCaseAndPhoneNumberNotAndInactiveAndIdNot(@Param("emailId") String emailId,
			@Param("phoneNumber") String phoneNumber, @Param("inactive") boolean inactive,
			@Param("excludeUserId") String excludeUserId);

	List<UserProfile> findByUsernameContainingIgnoreCase(String username);

	@Query("SELECT u FROM UserProfile u WHERE u.username = :username")
	List<UserProfile> findByUsernameOrPhoneNumber(String username);

	@Query("""
			    SELECT u FROM UserProfile u
			    WHERE u.userType = 'PLAYER'
			    AND u.id NOT IN (
			        SELECT tam.traineeUserProfile.id FROM TraineeAcademyMapping tam
			    )
			""")
	List<UserProfile> findPlayersNotLinkedToAnyAcademy();

}