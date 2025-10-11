CREATE TABLE user_profiles (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255),
    password_hashed VARCHAR(255),
    display_name VARCHAR(255),
    dob VARCHAR(255),
    user_type VARCHAR(255),
    google_advertising_id VARCHAR(255),
    email_id VARCHAR(255),
    phone_number VARCHAR(255),
    profile_picture_url VARCHAR(255),
    about_me TEXT,
    gender VARCHAR(255),
    created_on TIMESTAMP,
    inactive BOOLEAN,
    is_phone_number_verified BOOLEAN,
    is_email_id_verified BOOLEAN,
    android_fcm_push_token VARCHAR(255),
    default_password BOOLEAN,
    otp_hashed VARCHAR(255),
    experience_in_months INTEGER,
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    pincode VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    country VARCHAR(255),
    role VARCHAR(128)
);

CREATE TABLE profile_stats (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES user_profiles(id),
    total_games_played INTEGER,
    total_wins INTEGER,
    total_losses INTEGER,
    highest_score INTEGER,
    average_score DECIMAL(5, 2),
    followers BIGINT,
    following BIGINT,
    profile_views BIGINT,
    created_on TIMESTAMP,
    updated_on TIMESTAMP
);

CREATE TABLE user_preferred_sports_mappings (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES user_profiles(id),
    sport VARCHAR(255)
);

CREATE TABLE user_roles_mappings (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES user_profiles(id),
    role VARCHAR(255)
);


CREATE TABLE user_expertise_mappings (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES user_profiles(id),
    expertise VARCHAR(255),
    sport VARCHAR(255)
);

CREATE TABLE academies (
    id VARCHAR(255) PRIMARY KEY,
    internal_id VARCHAR(255),
    name VARCHAR(255),
    manager_user_id VARCHAR(255),
    inactive BOOLEAN,
    created_on TIMESTAMP,
    email_id VARCHAR(255),
    phone_number VARCHAR(255),
    headquarter VARCHAR(255),
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    pincode VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    country VARCHAR(255),
    start_time VARCHAR(255),
    end_time VARCHAR(255),
    icon_url VARCHAR(255)
);

CREATE TABLE branches (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    academy_id VARCHAR(255),
    inactive BOOLEAN,
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    country VARCHAR(255),
    pincode VARCHAR(255),
    phone_number VARCHAR(255),
    created_on TIMESTAMP,
    updated_on TIMESTAMP,
    FOREIGN KEY (academy_id) REFERENCES academies(id)
);

CREATE TABLE academy_admin_mappings (
    id SERIAL PRIMARY KEY,
    academy_id VARCHAR(255),
    user_id VARCHAR(255),
    created_at_timestamp_utc TIMESTAMP,
    FOREIGN KEY (academy_id) REFERENCES academies(id)
);

CREATE TABLE academy_sport_mappings (
    id SERIAL PRIMARY KEY,
    academy_id VARCHAR(255),
    sport VARCHAR(255),
    created_at_timestamp_utc TIMESTAMP,
    FOREIGN KEY (academy_id) REFERENCES academies(id)
);

CREATE TABLE coach_academy_mappings (
    id VARCHAR(255) PRIMARY KEY,
    coach_user_id VARCHAR(255),
    academy_id VARCHAR(255),
    created_on TIMESTAMP,
    updated_on TIMESTAMP,
    designation VARCHAR(255),
    experience_in_months INTEGER,
    status VARCHAR(255),
    last_status_update_epoch TIMESTAMP,
    FOREIGN KEY (coach_user_id) REFERENCES user_profiles(id),
    FOREIGN KEY (academy_id) REFERENCES academies(id)
);

CREATE TABLE trainee_academy_mappings (
    id VARCHAR(255) PRIMARY KEY,
    trainee_user_id VARCHAR(255),
    academy_id VARCHAR(255),
    created_on TIMESTAMP,
    updated_on TIMESTAMP,
    assigned_coach_user_id VARCHAR(255),
    status VARCHAR(255),
    last_status_update_time TIMESTAMP,
    FOREIGN KEY (trainee_user_id) REFERENCES user_profiles(id),
    FOREIGN KEY (academy_id) REFERENCES academies(id)
);

CREATE TABLE courses (
    id VARCHAR(255) PRIMARY KEY,
    created_on TIMESTAMP,
    title VARCHAR(255),
    description TEXT,
    coach_user_id VARCHAR(255),
    academy_id VARCHAR(255),
    level VARCHAR(255),
    total_max_trainees BIGINT,
    min_age BIGINT,
    max_age BIGINT,
    icon_url VARCHAR(255),
    inactive BOOLEAN,
    sport VARCHAR(255),
    visibility VARCHAR(255),
    FOREIGN KEY (coach_user_id) REFERENCES user_profiles(id),
    FOREIGN KEY (academy_id) REFERENCES academies(id)
);

CREATE TABLE course_coach_mappings (
    id SERIAL PRIMARY KEY,
    created_on TIMESTAMP,
    course_id VARCHAR(255),
    coach_user_id VARCHAR(255),
    FOREIGN KEY (course_id) REFERENCES courses(id),
    FOREIGN KEY (coach_user_id) REFERENCES user_profiles(id)
);

CREATE TABLE course_payments_option_mappings (
    id SERIAL PRIMARY KEY,
    course_id VARCHAR(255),
    payment_schedule VARCHAR(255),
    payment_amount BIGINT,
    currency VARCHAR(255),
    FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE course_rules_and_regulations_mappings (
    id SERIAL PRIMARY KEY,
    course_id VARCHAR(255),
    rule TEXT,
    FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE schedules (
    id SERIAL PRIMARY KEY,
    course_id VARCHAR(255),
    type VARCHAR(255),
    custom_dates_json TEXT,
    weekdays_json TEXT,
    start_date VARCHAR(255),
    end_date VARCHAR(255),
    start_time VARCHAR(255),
    end_time VARCHAR(255),
    timezone VARCHAR(255),
    FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE trainee_course_enrollments (
    id VARCHAR(255) PRIMARY KEY,
    academy_id VARCHAR(255),
    course_id VARCHAR(255),
    trainee_user_id VARCHAR(255),
    created_on TIMESTAMP,
    status VARCHAR(255),
    payment_schedule VARCHAR(255),
    amount bigint,
    FOREIGN KEY (academy_id) REFERENCES academies(id),
    FOREIGN KEY (course_id) REFERENCES courses(id),
    FOREIGN KEY (trainee_user_id) REFERENCES user_profiles(id)
);

CREATE TABLE course_attendances (
    id VARCHAR(255) PRIMARY KEY,
    academy_id VARCHAR(255),
    course_id VARCHAR(255),
    normalized_date BIGINT,
    attendance_json TEXT,
    created_on TIMESTAMP,
    updated_on TIMESTAMP,
    created_by_user_id VARCHAR(255),
    FOREIGN KEY (academy_id) REFERENCES academies(id),
    FOREIGN KEY (course_id) REFERENCES courses(id),
    FOREIGN KEY (created_by_user_id) REFERENCES user_profiles(id)
);

CREATE TABLE payments (
    id VARCHAR(255) PRIMARY KEY,
    trainee_course_enrollment_id VARCHAR(255),
    external_transaction_id VARCHAR(255),
    payment_mode VARCHAR(255),
    transaction_time VARCHAR(255),
    payment_status VARCHAR(255),
    amount BIGINT,
    currency VARCHAR(255),
    payment_schedule VARCHAR(255),
    payment_installment_date VARCHAR(255),
    next_due_at VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    extra_args TEXT,
    receipt_id VARCHAR(255),
    payment_initiated_by_user_id VARCHAR(255),
    receipt_url TEXT,
    FOREIGN KEY (payment_initiated_by_user_id) REFERENCES user_profiles(id),
    FOREIGN KEY (trainee_course_enrollment_id) REFERENCES trainee_course_enrollments(id)
);

CREATE TABLE trainee_performance_reports (
    id VARCHAR(255) PRIMARY KEY,
    trainee_user_id VARCHAR(255),
    coach_user_id VARCHAR(255),
    academy_id VARCHAR(255),
    course_id VARCHAR(255),
    title VARCHAR(255),
    sport VARCHAR(50),
    created_on TIMESTAMP,
    updated_on TIMESTAMP,
    status VARCHAR(50),
    report_json TEXT,
    FOREIGN KEY (trainee_user_id) REFERENCES user_profiles(id),
    FOREIGN KEY (coach_user_id) REFERENCES user_profiles(id),
    FOREIGN KEY (academy_id) REFERENCES academies(id),
    FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE coach_performance_reports (
    id VARCHAR(255) PRIMARY KEY,
    trainee_user_id VARCHAR(255),
    coach_user_id VARCHAR(255),
    academy_id VARCHAR(255),
    course_id VARCHAR(255),
    title VARCHAR(255),
    sport VARCHAR(50),
    created_on TIMESTAMP,
    updated_on TIMESTAMP,
    status VARCHAR(50),
    report_json TEXT,
    FOREIGN KEY (trainee_user_id) REFERENCES user_profiles(id),
    FOREIGN KEY (coach_user_id) REFERENCES user_profiles(id),
    FOREIGN KEY (academy_id) REFERENCES academies(id),
    FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE trainee_performance_report_media_mappings (
    id VARCHAR(255) PRIMARY KEY,
    report_id VARCHAR(255),
    media_url TEXT,
    FOREIGN KEY (report_id) REFERENCES trainee_performance_reports(id)
);

CREATE TABLE courts (
    id VARCHAR(255) PRIMARY KEY,
    academy_id VARCHAR(255),
    court_name VARCHAR(255),
    created_on TIMESTAMP,
    is_inactive BOOLEAN,
    FOREIGN KEY (academy_id) REFERENCES academies(id)
);

CREATE TABLE badminton_tournaments (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    inactive BOOLEAN,
    created_on TIMESTAMP,
    start_date VARCHAR(255),
    end_date VARCHAR(255),
    type VARCHAR(255),
    format VARCHAR(255),
    academy_id VARCHAR(255),
    created_by_user_id VARCHAR(255),
    FOREIGN KEY (academy_id) REFERENCES academies(id),
    FOREIGN KEY (created_by_user_id) REFERENCES user_profiles(id)
);


CREATE TABLE badminton_matches (
    id VARCHAR(255) PRIMARY KEY,
    academy_id VARCHAR(255),
    court_id VARCHAR(255),
    start_time VARCHAR(255),
    end_time VARCHAR(255),
    max_points INTEGER,
    max_rounds INTEGER,
    scorer_user_id VARCHAR(255),
    referee_user_id VARCHAR(255),
    created_at_timestamp_utc TIMESTAMP,
    updated_at_timestamp_utc TIMESTAMP,
    match_status VARCHAR(255),
    is_livestreamed BOOLEAN,
    is_recording_enabled BOOLEAN,
    game_format VARCHAR(255),
    inactive BOOLEAN,
    scheduled_start_time VARCHAR(255),
    badminton_tournament_id VARCHAR(255),
    streaming_status VARCHAR(32),
    streaming_channel VARCHAR(255),
    streaming_token VARCHAR(512),
    created_by_user_id VARCHAR(255),
    is_autogenerated BOOLEAN default false,
    FOREIGN KEY (academy_id) REFERENCES academies(id),
    FOREIGN KEY (court_id) REFERENCES courts(id),
    FOREIGN KEY (scorer_user_id) REFERENCES user_profiles(id),
    FOREIGN KEY (referee_user_id) REFERENCES user_profiles(id),
    FOREIGN KEY (badminton_tournament_id) REFERENCES badminton_tournaments(id),
    FOREIGN KEY (created_by_user_id) REFERENCES user_profiles(id)
);

CREATE TABLE badminton_singles_player_mapping (
    id SERIAL PRIMARY KEY,
    match_id VARCHAR(255),
    player_user_id VARCHAR(255),
    guest_name VARCHAR(255),
    FOREIGN KEY (match_id) REFERENCES badminton_matches(id),
    FOREIGN KEY (player_user_id) REFERENCES user_profiles(id)
);

CREATE TABLE teams (
    id VARCHAR(255) PRIMARY KEY,
    team_name VARCHAR(255),
    academy_id VARCHAR(255),
    badminton_tournament_id VARCHAR(255),
    created_on TIMESTAMP,
    inactive BOOLEAN,
    created_by_user_id VARCHAR(255),
    FOREIGN KEY (academy_id) REFERENCES academies(id),
    FOREIGN KEY (badminton_tournament_id) REFERENCES badminton_tournaments(id),
    FOREIGN KEY (created_by_user_id) REFERENCES user_profiles(id);
);

CREATE TABLE badminton_team_mapping (
    id SERIAL PRIMARY KEY,
    match_id VARCHAR(255),
    team_id VARCHAR(255),
    FOREIGN KEY (match_id) REFERENCES badminton_matches(id),
    FOREIGN KEY (team_id) REFERENCES teams(id)
);



CREATE TABLE team_players (
    id SERIAL PRIMARY KEY,
    team_id VARCHAR(255),
    player_user_id VARCHAR(255),
    guest_player_name VARCHAR(255),
    FOREIGN KEY (team_id) REFERENCES teams(id),
    FOREIGN KEY (player_user_id) REFERENCES user_profiles(id)
);

CREATE TABLE groups (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    academy_id VARCHAR(255),
    created_on TIMESTAMP,
    inactive BOOLEAN,
    created_by_user_id VARCHAR(255),
    FOREIGN KEY (academy_id) REFERENCES academies(id)
);

CREATE TABLE group_admin_mappings (
    id SERIAL PRIMARY KEY,
    group_id VARCHAR(255),
    group_admin_user_id VARCHAR(255),
    created_on TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES groups(id),
    FOREIGN KEY (group_admin_user_id) REFERENCES user_profiles(id)
);

CREATE TABLE group_member_mappings (
    id SERIAL PRIMARY KEY,
    group_id VARCHAR(255),
    group_member_user_id VARCHAR(255),
    created_on TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES groups(id),
    FOREIGN KEY (group_member_user_id) REFERENCES user_profiles(id)
);

CREATE TABLE enquiries (
    id VARCHAR(255) PRIMARY KEY,
    description TEXT,
    user_id VARCHAR(255),
    course_id VARCHAR(255),
    status VARCHAR(50),
    notes TEXT,
    created_on TIMESTAMP,
    updated_on TIMESTAMP,
    academy_id VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES user_profiles(id),
    FOREIGN KEY (course_id) REFERENCES courses(id),
    FOREIGN KEY (academy_id) REFERENCES academies(id)
);

CREATE TABLE badminton_match_play_details (
    id SERIAL PRIMARY KEY,
    match_id VARCHAR(255) REFERENCES badminton_matches(id),
    tournament_id VARCHAR(255) REFERENCES badminton_tournaments(id),
    winning_player_id VARCHAR(255) REFERENCES user_profiles(id),
    winning_team_id VARCHAR(255) REFERENCES teams(id),
    winning_guest_player_name VARCHAR(255),
    is_tied BOOLEAN default false
);

CREATE TABLE badminton_match_rounds (
    id SERIAL PRIMARY KEY,
    match_play_details_id BIGINT REFERENCES badminton_match_play_details(id),
    round_start_time TIMESTAMP,
    round_end_time TIMESTAMP,
    round_number INTEGER,
    winning_user_id VARCHAR(255) REFERENCES user_profiles(id),
    winning_team_id VARCHAR(255) REFERENCES teams(id),
    winning_guest_player_name VARCHAR(255)
);

CREATE TABLE badminton_match_round_play_details (
    id SERIAL PRIMARY KEY,
    match_round_id BIGINT REFERENCES badminton_match_rounds(id),
    player_user_id VARCHAR(255) REFERENCES user_profiles(id),
    team_id VARCHAR(255) REFERENCES teams(id),
    guest_player_name VARCHAR(255),
    score INTEGER
);

CREATE TABLE badminton_tournament_gallery_media (
    id SERIAL PRIMARY KEY,
    tournament_id VARCHAR(255) NOT NULL,
    media_path VARCHAR,
    media_type VARCHAR(50),
    created_on TIMESTAMP,
    CONSTRAINT fk_tournament
        FOREIGN KEY(tournament_id)
        REFERENCES badminton_tournaments(id)
);

CREATE TABLE badminton_tournament_players (
    id SERIAL PRIMARY KEY,
    player_user_id VARCHAR(255) NOT NULL,
    badminton_tournament_id VARCHAR(255) NOT NULL,
    created_on TIMESTAMP,
    CONSTRAINT fk_player_user
        FOREIGN KEY(player_user_id)
        REFERENCES user_profiles(id),
    CONSTRAINT fk_badminton_tournament
        FOREIGN KEY(badminton_tournament_id)
        REFERENCES badminton_tournaments(id)
);

CREATE TABLE reported_posts (
    id BIGSERIAL PRIMARY KEY,
    academy_id VARCHAR(255),
    reported_by VARCHAR(255),
    created_on TIMESTAMP,
    updated_on TIMESTAMP,
    status VARCHAR(50),
    last_status_update_epoch TIMESTAMP,
    post_id VARCHAR(255),

    -- Foreign keys based on your entity relationships
    CONSTRAINT fk_academy FOREIGN KEY(academy_id) REFERENCES academies(id),
    CONSTRAINT fk_user_profile FOREIGN KEY(reported_by) REFERENCES user_profiles(id)
);

-- Optional indexes for better query performance
CREATE INDEX idx_reported_posts_academy_id ON reported_posts(academy_id);
CREATE INDEX idx_reported_posts_status ON reported_posts(status);
CREATE INDEX idx_reported_posts_post_id ON reported_posts(post_id);