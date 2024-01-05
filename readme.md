# 사용 기술
- JAVA 17
- SpringBoot 3.0.5
- MySQL 8.0
- Nginx Ingress Controller 1.8.2

# 주요 파일 경로
- build/docker/Dockerfile : 컨테이너 이미지 제작에 사용된 Dockerfile
- kubernetes-manifest/app.yaml : 어플리케이션의 Deployment, Service, Ingress를 생성하기 위한 Manifest
- kubernetes-manifest/db.yaml : DB의 Deployment, Service를 생성하기 위한 Manifest
- build.gradle : gradle에서 컨테이너 이미지 빌드를 수행하기 위한 스크립트 작성

## 어플리케이션 빌드
- Fork를 한 spring-petclinic-data-jdbc 어플리케이션이 Maven 프로젝트로 제작이 되어 있었음.
- 과제의 요구사항을 위해 프로젝트를 Gradle로 변경.
- gradlew clean build 명령어를 사용해서 jar파일을 생성.

## Kubernetes 배포
1. 어플리케이션의 log는 host의 `/logs` 에 적재되도록 합니다.
- 어플리케이션의 log를 host 노드의 /logs에 적재하기 위해 POD의 볼륨을 hostPath 방식으로 사용.
- 컨테이너 내부의 /logs에 먼저 로깅파일이 적재되도록 JVM 시작 옵션으로 -Dlogging.file.name=/logs/spring-petclinic.log 을 사용.
- K8s의 Deployment Manifest 생성시 볼륨 마운트 옵션에서 컨테이너의 /logs를 host노드의 /logs로 마운트.

2. 정상 동작 여부를 반환하는 api를 구현하며, 10초에 한번씩 체크합니다.
- POD의 정상동작 여부를 판단하기 위해 헬스체크 API를 org.springframework.samples.petclinic.kubernetes.HealthCheckController 에 구현했음
- http://서비스아이피/kubernetes/health 주소로 GET 요청이 들어오면 ok라는 텍스트를 반환 하는 RestController로 제작함.
- POD의 서비스 상태를 확인하는 kubernetes의 readinessProbe를 활용했고 periodSeconds의 값을 10으로 설정해서 10초마다 상태를 확인 하도록 함.

3. 종료 시 30초 이내에 프로세스가 종료되지 않으면 SIGKILL로 강제 종료합니다.
- kubectl delete pod [파드이름] 으로 POD를 삭제시 기본적으로 SIGTERM으로 동작하며 어플리케이션의 안전한 종료를 위해 기본값 30초간 대기하게 되어있음.
- 30초 대기후 SIGKILL 신호를 보내어 POD를 즉시 종료함.
- 안전한 종료를 위한 대기시간을 명시적으로 표시하기 위해 Manifest에 terminationGracePeriodSeconds: 30 옵션을 주었음.

4. 배포 시 scale-in, out 상황에서 유실되는 트래픽은 없어야 합니다.
- scale-in 시에 트래픽 유실을 방지하기 위해 readinessProbe을 활용하여 POD가 읽기 상태에 진입하면 서비스에서 트래픽 전송을 하게함.
- scale-out 시에 트래픽 유실을 방지하기 위해 SIGTERM의 대기시간을 terminationGracePeriodSeconds 을 활용하여 명시적으로 표현함.

5.어플리케이션 프로세스는 root 계정이 아닌 uid:999로 실행합니다.
- root가 아닌 uid:999를 가진 유저로 어플리케이션을 실행하기 위해 Dockerfile(파일경로 build/docker/Dockerfile) 내에 RUN useradd -u $UID han2000w 로 유저를 생성.

6. DB도 kubernetes에서 실행하며 재 실행 시에도 변경된 데이터는 유실되지 않도록 설정합니다.
- MySQL 8.0 을 POD로 실행했으며 컨테이너 내의 DB 데이터가 저장되는 /var/lib/mysql 경로를 hostPath를 활용하여 host노드의 /var/lib/mysql에 마운트함.

7. 어플리케이션과 DB는 cluster domain으로 통신합니다.
- 어플리케이션이 cluster domain을 통해 DB와 통신할 수 있도록 어플리케이션의 application.properties파일에 DB의 쿠버네티스 Service URL을 작성함.
- 다음과 같이 datasource url을 서비스의 클러스터 도메인으로 명시함. spring.datasource.url=jdbc:mysql://mysql-svc.default.svc.cluster.local:3306/petclinic

8. ingress-controller를 통해 어플리케이션에 접속이 가능해야 합니다.
- 과제 작성을 위한 Baremetal 환경의 K8s에서 Ingress를 사용하기 위해 Nginx Ingress Controller를 사용함.
- Ingress파일의 Rule에 명시된 han2000w.com 호스트로 접속시 같은 네임스페이스의 spring-petclinic-data-jdbc-svc:8080 서비스로 연결이 되도록 설정.

9. namespace는 default를 사용합니다.
- 과제에 사용된 모든 리소스들의 namespace를 default로 사용하였음.
