#!/bin/bash

# ORV API Server Load Test Data Setup Script
# Phase 0: 부하테스트 데이터 준비 자동화 스크립트

set -e  # 에러 발생시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 설정 변수 (환경에 맞게 수정)
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-orv_api}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-password}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_DIR="$SCRIPT_DIR/sql"

# PostgreSQL 연결 확인
check_db_connection() {
    log_info "데이터베이스 연결 확인 중..."
    
    if ! PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1;" > /dev/null 2>&1; then
        log_error "데이터베이스 연결 실패. 다음 설정을 확인하세요:"
        echo "  Host: $DB_HOST"
        echo "  Port: $DB_PORT" 
        echo "  Database: $DB_NAME"
        echo "  User: $DB_USER"
        exit 1
    fi
    
    log_success "데이터베이스 연결 성공"
}

# SQL 스크립트 실행
execute_sql_script() {
    local script_name=$1
    local script_path="$SQL_DIR/$script_name"
    
    if [ ! -f "$script_path" ]; then
        log_error "스크립트 파일을 찾을 수 없습니다: $script_path"
        exit 1
    fi
    
    log_info "$script_name 실행 중..."
    
    if ! PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$script_path"; then
        log_error "$script_name 실행 실패"
        exit 1
    fi
    
    log_success "$script_name 실행 완료"
}

# 기존 테스트 데이터 확인
check_existing_test_data() {
    log_info "기존 테스트 데이터 확인 중..."
    
    local count=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM member WHERE provider = 'test';" | tr -d ' ')
    
    if [ "$count" -gt 0 ]; then
        log_warning "기존 테스트 사용자 $count 명이 발견되었습니다."
        read -p "기존 테스트 데이터를 삭제하고 새로 생성하시겠습니까? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            execute_sql_script "99_cleanup_test_data.sql"
        else
            log_info "기존 데이터를 유지하고 계속 진행합니다."
        fi
    else
        log_info "기존 테스트 데이터가 없습니다. 새로 생성합니다."
    fi
}

# 스토리보드 데이터 확인
check_storyboard_data() {
    log_info "스토리보드 데이터 확인 중..."
    
    local count=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM storyboard;" | tr -d ' ')
    
    if [ "$count" -lt 8 ]; then
        log_warning "스토리보드가 $count 개만 발견되었습니다. 최소 8개가 필요합니다."
        log_warning "실제 운영 데이터가 있는지 확인하고 필요시 초기 데이터를 삽입하세요."
    else
        log_success "스토리보드 $count 개 확인"
    fi
}

# 애플리케이션 빌드 및 테스트
test_application_build() {
    log_info "loadtest 프로파일로 애플리케이션 빌드 테스트 중..."
    
    cd "$SCRIPT_DIR/../../../.."  # 프로젝트 루트로 이동
    
    if ! ./gradlew build -x test; then
        log_error "애플리케이션 빌드 실패"
        exit 1
    fi
    
    log_success "애플리케이션 빌드 성공"
}

# 테스트 인증 동작 확인
test_auth_functionality() {
    log_info "테스트 인증 기능 확인 중..."
    
    # 애플리케이션이 실행 중인지 확인
    if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        log_warning "애플리케이션이 실행 중이지 않습니다."
        log_info "다음 명령어로 loadtest 프로파일로 애플리케이션을 시작하세요:"
        echo "  ./gradlew bootRun --args='--spring.profiles.active=loadtest'"
        return
    fi
    
    # 테스트 인증 API 호출
    local response=$(curl -s -w "%{http_code}" http://localhost:8080/api/v0/auth/callback/test?code=test_user_1)
    local http_code="${response: -3}"
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "302" ]; then
        log_success "테스트 인증 API 동작 확인"
    else
        log_warning "테스트 인증 API 응답 코드: $http_code"
        log_warning "애플리케이션이 loadtest 프로파일로 실행되었는지 확인하세요."
    fi
}

# 메인 함수
main() {
    echo "=========================================="
    echo "ORV API 부하테스트 데이터 준비 스크립트"
    echo "Phase 0: 필수 보완사항 해결"
    echo "=========================================="
    echo
    
    # 필수 검증
    check_db_connection
    check_storyboard_data
    
    # 기존 데이터 확인 및 정리
    check_existing_test_data
    
    # 테스트 데이터 생성
    execute_sql_script "01_create_test_users.sql"
    execute_sql_script "02_create_recap_data.sql"
    
    # 애플리케이션 테스트
    test_application_build
    test_auth_functionality
    
    echo
    echo "=========================================="
    log_success "Phase 0 완료!"
    echo "=========================================="
    echo
    echo "다음 단계:"
    echo "1. 애플리케이션을 loadtest 프로파일로 실행:"
    echo "   ./gradlew bootRun --args='--spring.profiles.active=loadtest'"
    echo
    echo "2. 테스트 인증 확인:"
    echo "   curl 'http://localhost:8080/api/v0/auth/callback/test?code=test_user_1'"
    echo
    echo "3. Phase 1 진행: nGrinder 프로젝트 설정"
    echo
}

# 도움말 표시
show_help() {
    echo "사용법: $0 [옵션]"
    echo
    echo "옵션:"
    echo "  -h, --help     이 도움말 표시"
    echo "  --cleanup-only 테스트 데이터만 정리하고 종료"
    echo "  --skip-build   애플리케이션 빌드 건너뛰기"
    echo
    echo "환경 변수:"
    echo "  DB_HOST        데이터베이스 호스트 (기본값: localhost)"
    echo "  DB_PORT        데이터베이스 포트 (기본값: 5432)"
    echo "  DB_NAME        데이터베이스 이름 (기본값: orv_api)"
    echo "  DB_USER        데이터베이스 사용자 (기본값: postgres)"
    echo "  DB_PASSWORD    데이터베이스 비밀번호 (기본값: password)"
    echo
}

# 명령행 인수 처리
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        --cleanup-only)
            check_db_connection
            execute_sql_script "99_cleanup_test_data.sql"
            log_success "테스트 데이터 정리 완료"
            exit 0
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        *)
            log_error "알 수 없는 옵션: $1"
            show_help
            exit 1
            ;;
    esac
done

# 메인 실행
main
