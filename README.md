# cooking-app-spring
## Eco Recipe 사이트

- 제목 : Eco Recipe 웹 프로젝트

- 프로젝트 : 팀 프로젝트
  - 프로젝트 종류 : 안드로이드 앱, iOS, 웹
  - 팀 : [대학 캡스톤 팀 1조](https://github.com/college-capstone-team-1)
  - 깃헙 주소 : [cooking web project](https://github.com/college-capstone-team-1/cooking-app-spring)
  - 배포된 사이트 : [Recipe](http://www.inndiary.xyz)

- 개발 언어 : Java

- 프레임워크 : Spring, Spring Boot

- 개발 의존 : Spring Security, Spring MVC, Thymeleaf, JPA, QueryDSL, OAuth2.0

- 개발 환경 : intellij IDEA, HeidiSQL, AWS EC2, GCP(Google Cloud Platform)

- DB : MariaDB

- 기간 : 2022. 10. ~ 12.

- 배포 환경 : Amazone EC2, Amazone Route 53

- 로직 설계 : Notion, Figma

프론트엔드, 백엔드 전부 제가 개발했습니다.

프론트는 Bootstrap과 이미 개발된 template에 thymeleaf를 통해 수정했습니다.

## 사용 API
     http://www.foodsafetykorea.go.kr/api/openApiInfo.do?menu_grp=MENU_GRP31&menu_no=661&show_cnt=10&start_idx=1&svc_no=COOKRCP01
---

# 요구사항
- API 서비스 제공
  - 안드로이드, iOS를 위한 Json API 제공
    - 모든 레시피를 페이징 처리(page)해서 보내는 양(size)을 지정하고 정렬 순서(order)를 정하고 요청할 수 있는 API
      - 순서 : 좋아요 많은 순, 일반순
    - 위와 같은 API에 And 연산 Query 검색이 가능한 API
      - 검색 가능 종류 :
        - 이름 (name), 조리방식 (way), 요리분류 (part), 재료 (detail), 레시피 순번 (sequence)
    - 좋아요 조회, 추가와 삭제 API
  - 웹 서비스용 API 제공
    - 좋아요를 동적으로 제공하기 위한 API

- 웹 서비스
  - 메인화면에서 사용자들이 좋아한 레시피 top 8가지를 보여줌
  - 제일 많은 좋아요를 받은 레시피와 관련된 카테고리의 레시피를 무작위로 추천
  - 모든 레시피를 조회 가능
    - 카테고리를 통해 레시피를 원하는 조건에 맞게 조회 가능
      - 이름, 조리방식, 요리분류, 재료
        - 이름, 재료는 텍스트 검색
        - 조리방식, 요리분류는 선택 검색
  - 레시피에 대해 상세하게 볼 수 있음.
    - 원하는 레시피에 좋아요를 누를 수 있음
    - 상세히 누른 레시피 하단에 관련 카테고리 레시피 무작위로 추천
  - 로그인 사용자는 좋아요한 레시피와 사용자 정보를 조회할 수 있음
    - 앱, iOS 유저는 동일한 구글 로그인이 가능하다면 연동해 조회 가능

---

### 도메인
![](recipe/img/레시피_도메인_결과물.png)

### 설계

#### 초안
![](recipe/img/레시피_로직_초안.png)

#### 개발 완료 후
![](recipe/img/레시피_로직_결과물.png)

# 로직

### OpenAPI
![](recipe/img/레시피_로직_OpenAPI.png)

### 로그인
![](recipe/img/레시피_로직_로그인.png)

---

# 구현과 코드

## OpenAPI 값 받아오기

### 코드 상세

#### OpenAPI Handler
```java
/*
        OpenAPIHandler 코드
        API 요청을 받아 OpenAPIProvider에서 공공데이터로 실질적으로 데이터를 요청하고 값을 받아옴
        받은 데이터가 옳바른지 확인하고 OpenAPIParser를 통해 Json String에서 openRecipe로 변환해 객체를 반환
 */
@Component
public class OpenAPIHandler {
    private final OpenAPIProvider openApiProvider = OpenAPIProvider.getInstance();
    // String 데이터를 객체로 집어 넣음
    private final OpenAPIParser apiParser = OpenAPIParser.getInstance();
    private final OpenAPIErrorHandler openApiErrorHandler = new OpenAPIErrorHandler();

    // 1번의 요청 시 가져올 값
    private static final int MAXIMUM_REQUEST = 1000;
    private int totalIndex = 0;

    // 공공 데이터의 1부터 끝까지의 데이터 요청
    public List<OpenAPIRecipe> requestAllOpenAPI() {
        List<OpenAPIRecipe> openAPIRecipes = new ArrayList<>();
        List<OpenAPIRecipe> resultOfFetchValues = startScanOpenAPI(openAPIRecipes);
        return resultOfFetchValues;
    }

    private List<OpenAPIRecipe> startScanOpenAPI(List<OpenAPIRecipe> openAPIRecipes) {
        int endSearch = MAXIMUM_REQUEST;
        setTotalIndex();
        
        // 설정한 최대값까지 모든 레시피 데이터를 요청
        for (int search = 1; search < totalIndex; search += MAXIMUM_REQUEST) {
            openApiProvider.urlIndexRangeScan(search, endSearch);
            OpenAPIRecipe requestCR = cookRecipeRequest(openApiProvider.getApi().getAPIUrl());

            // 검증된 뒤 객체에 담긴 레시피를 리스트에 추가 
            openAPIRecipes.add(requestCR);
            endSearch += MAXIMUM_REQUEST;
        }
        // 조회된 모든 레시피 결과 반환
        return openAPIRecipes;
    }

    // API 요청에 특정 값 요청 시 총 몇 개의 레시피가 있는지 알려주는 값을 최대 값에 넣어줌
    private void setTotalIndex() {
        int searchTotalNumber = 1;
        openApiProvider.urlIndexRangeScan(searchTotalNumber, searchTotalNumber);
        OpenAPIRecipe totalSearch = cookRecipeRequest(openApiProvider.getApi().getAPIUrl());
        this.totalIndex = Integer.parseInt(totalSearch.getTotalCount());
    }

    // url로부터 받은 String Json에서 openRecipe 객체로 변환한 값에 이상이 없는지 확인
    private OpenAPIRecipe cookRecipeRequest(URL apiUrl) {
        try {
            OpenAPIRecipe needValueCheck = requestOpenAPIFromURL(apiUrl);
            return openApiErrorHandler.cookRecipeRightValueCheck(needValueCheck);
        } catch (Exception e) {
            throw new NullPointerException();
        }
    }

    // url 값으로 받아 Json String 값을 openRecipe 객체로 변환
    private OpenAPIRecipe requestOpenAPIFromURL(URL apiUrl) {
        try {
            return apiParser.parseURLToCookRecipe(apiUrl);
        } catch (IOException e) {
            e.getStackTrace();
            log.error("wrong json value : can't parse Json to String need to check");
            throw new IllegalArgumentException();
        }
    }
}
```

공공데이터로부터 데이터를 가져옴(OpenAPIProvider) -> 데이터 이상여부 확인(OpenAPIErrorHandler) -> 사용가능한 객체로 담아(OpenAPIParser, OpenAPIDelegator) 전달하는 객체로

#### OpenAPI Provider
```java
/*
        공공 데이터에 접속해 데이터를 요청하는 객체
        url과 해당 데이터 요청 API키 코드를 통해 데이터를 가져온다. 
 */
class OpenAPIProvider {
    private static final OpenAPIProvider INSTANCE = new OpenAPIProvider();
    private static final String RECIPE_OPEN_API = "https://openapi.foodsafetykorea.go.kr";
    private static final String API_KEY = "ac3c23441c1c4a1e9696";
  
    private OpenAPI openApi;
  
    // 여러 객체가 필요하지 않기 때문에 다른 클래스에서 객체 생성 불가능하도록 설정
    private OpenAPIProvider() { }
  
    // 싱글턴 패턴으로 새로운 객체를 생성할 필요가 없기 때문에 인스턴스 생성을 해두고 해당 객체를 반환
    public static OpenAPIProvider getInstance() {
        return INSTANCE;
    }
  
    // 인덱스에 따라 데이터를 요청
    private URL requestOpenAPIJSON(int startIndex, int endIndex) throws MalformedURLException {
        return new URL(RECIPE_OPEN_API + "/api/" + API_KEY +
                "/COOKRCP01/json/" + startIndex + "/" + endIndex);
    }
    
    // 시작 인덱스, 끝 인덱스를 설정해도 이상이 없는지 확인 (요청 시 정상적인지)
    public void urlIndexRangeScan(int startIndex, int endIndex) {
        try {
            openApi = new OpenAPI(requestOpenAPIJSON(startIndex, endIndex));
            log.info("test of open Api : {}", openApi.getAPIUrl().toString());
        }  catch (MalformedURLException mal) {
              mal.printStackTrace();
              log.error("wrong url or wrong api key");
        }
    }
  
    public OpenAPI getApi() {
        return openApi;
    }
}
```

#### OpenAPI Delegator
```java
public class OpenAPIDelegator {
    public static OpenRecipe rowToOpenRecipe(Row row) {
        return OpenRecipe.builder()
                .attFileNoMain(insteadEmptyString(row.getAttFileNoMain())).attFileNoMk(insteadEmptyString(row.getAttFileNoMk()))
                .hashTag(insteadEmptyString(row.getHashTag()))
                .infoCar(doubleFormatting(row.getInfoCar())).infoEng(doubleFormatting(row.getInfoEng()))
                .infoFat(doubleFormatting(row.getInfoFat())).infoNa(doubleFormatting(row.getInfoNa()))
                .infoPro(doubleFormatting(row.getInfoPro())).infoWgt(doubleFormatting(row.getInfoWgt()))
                .manual01(insteadEmptyString(row.getManual01()))....manual20(insteadEmptyString(row.getManual20()))
                .manualImg01(insteadEmptyString(row.getManualImg01()))....manualImg20(insteadEmptyString(row.getManualImg20()))
                .rcpNm(insteadEmptyString(row.getRcpNm())).rcpSeq(Long.parseLong(row.getRcpSeq())).rcpWay2(insteadEmptyString(row.getRcpWay2()))
                .rcpPat2(insteadEmptyString(row.getRcpPat2())).rcpPartsDtls(insteadEmptyString(row.getRcpPartsDtls()))
                .build(); // 공공 데이터에서 메뉴얼, 메뉴얼 이미지를 20개나 만들었기 때문에 너무 긴 나머지 해당 값의 전달 생략 1부터 20까지 메뉴얼
    }
  
    /*
            이미지가 비었는지, 비어있지 않는지 확인
            이유는 해당 값을 저장하고 DB에서 꺼내 앱에 Json 객체로 전달해야 하는데, 값이 없는 경우 null 값이 넘어가고,
            해당 값이 null로 넘어가면 안드로이드에서 그 값을 String이 전달 됐을 때 타입이 아닌 null 관련 타입으로 저장하게 되고
            이로 인해 나중에 String 값이 저장되면 오류가 발생
            
            해결을 위해 전달받는 값이 비었는지 확인 후 ""를 넣도록 확인하는 함수
     */
    private static String imageCheckString(String value) {
        String defaultImage = "http://www.foodsafetykorea.go.kr/"; // 이미지의 값은 링크인데, 이 링크만 전달되는 경우가 있음 의미가 없는 값인 듯 해서 ""로 전달하게 바꿈
        String check = insteadEmptyString(value);
    
        if (check.equals(defaultImage)) {
            return "";
        }
        return check;
    }
  
    // String 값은 위의 사진의 예시와 같이 전부 해당 처리를 해주었다.
    private static String insteadEmptyString(String value) {
        if (value.isEmpty()) {
            return "";
        } else {
            return value;
        }
    }
  
    /*
            위의 String 값들과 같은 이유로 공공데이터의 double 값이 String으로 넘어오는데 
            이때 어떤 값은 .0 같은 double인데, int 값도 종종 넘어와서 형변환이 쉽지 않음
            
            그렇기 때문에 String에 .이 있는지 확인하고 형변환을 함
             빈값인 경우엔 0.0 값을 넣어줌
     */
    private static Double doubleFormatting(String value) {
        boolean isContains = value.contains(".");
        boolean isEmpty = (value.length() == 0);
        if (isEmpty) {
            return 0.0;
        }
    
        if (!isContains) {
            return (double) Integer.parseInt(value);
        }
        else {
            return Double.parseDouble(value);
        }
    }
}
```

해당 open API는 오류 발생 시 result 객체에 특정 코드를 담음, 해당 코드에 따라, 요청 데이터가 row에 있는지에 따라 오류 출력하는 클래스

#### OpenAPI Delegator
```java
public class OpenAPIErrorHandler {
    // 시작 인덱스와 끝 인덱스가 반대로 된 경우와 더이상 데이터가 없는 경우
    public OpenAPIRecipe cookRecipeRightValueCheck(OpenAPIRecipe openAPIRecipe) {
        if (openAPIRecipe.getResult().getMsg().equals(OpenAPICode.ERROR_334.getCode())) {
            log.error("wrong index position : start index is bigger then end index");
            throw new IllegalArgumentException();
        } else if (openAPIRecipe.getResult().getMsg().equals(OpenAPICode.INFO_200.getCode())) {
            log.error("no more data found : API data is no more");
            throw new IllegalArgumentException();
        } else {
            return cookRecipeNullCheck(openAPIRecipe);
        }
    }
  
    // 데이터가 아무것도 없이 비어있는 경우
    private OpenAPIRecipe cookRecipeNullCheck(OpenAPIRecipe openAPIRecipe) {
        if (openAPIRecipe.getResult().getMsg().isEmpty()) {
            log.error("no data found : object has no value error");
            throw new NullPointerException();
        } else {
            return openAPIRecipe;
        }
    }
  
    // 데이터가 아무것도 없이 비어있는 경우
    public OpenAPIRecipe cookRecipeValueCheck(OpenAPIRecipe openAPIRecipe) {
        if (openAPIRecipe.getRow().isEmpty()) {
            log.error("empty value requested : value is lost");
            throw new NullPointerException();
        } else {
            return openAPIRecipe;
        }
    }
  
    // 데이터가 아무것도 없이 비어있는 경우
    public OpenAPIRecipe cookRecipeInnerValueCheck(OpenAPIRecipe openAPIRecipe) {
        if (openAPIRecipe.getRow().get(0).getRcpSeq() == null) {
            log.error("empty value requested : inside Row value is lost");
            throw new NullPointerException();
        } else {
            return openAPIRecipe;
        }
    }
}
```

## API
### 앱 / 어플 통신용 API 구현
- 웹에서 API를 구현해 앱과 iOS에 레시피 데이터를 JSON을 통해 전달
- 공통 쿼리 : page, size, order (d : 일반순, r : 인기순)
- 모든 레시피 조회 : `/api/v1`
  - 검색 조건 쿼리 : name (String), detail (String), part (String), way (String), seq (Long)
  - and 검색 레시피 조회 : `/api/v1/search/find-only`
  - or 검색 레시피 조회 : `/api/v1/search/find-with`
- 좋아요 많은 순서로 8개 레시피 조회 : `/api/v1/recipes/rank`
- 테스트용 임시 API
  - 오픈 DB로부터 모든 데이터 저장 : `/api/v2/save`
  - 현재 DB의 모든 레시피 데이터 삭제 : `/api/v2/delete/all`

#### OpenAPIController
```java
@RequestMapping("/api")
@RestController
public class OpenAPIController {
    // query param에 값이 없이 전달 받을 경우 사용될 기본 값
    private final String DEFAULT_PAGE = "1";
    private final String DEFAULT_SIZE = "10";
    private final String DEFAULT_ORDER = "d";
  
    /*
            모든 레시피를 조회하는 API로 page, size, order 3가지 parameter 지원
            기본값 : 0, "d"로 parameter 값을 주지 않아도 사용 가능
     */
    @GetMapping(value = "/v1", produces = "application/json; charset=UTF-8")
    public RecipeData responseOpenAPI(@RequestParam(defaultValue = DEFAULT_PAGE) int page, @RequestParam(defaultValue = DEFAULT_SIZE) int size, @RequestParam(defaultValue = DEFAULT_ORDER) String order) {
        Sort sort = order.equals("f") ? Sort.by("favorite").descending() : Sort.by("id").ascending(); // 정렬 d 일 경우 기본 순, f 일 경우 favorite - 좋아요 많은 순
        PageRequest pageRequest = searchWithPageHandler.choosePage(page, size, sort);
    
        // 페이지 설정을 보내 해당 설정에 맞게 모든 레시피 조회
        APIPageResult<OpenRecipe, OpenRecipeEntity> openRecipeAPIPageResult = openRecipePageWithSearchService.allAPIDataSources(pageRequest);
    
        // 페이지의 끝인지, 최종 페이지는 몇 페이지인지, 총 몇 개가 찾아졌는지에 대한 정보
        boolean isEnd = page == TotalValue.getTotalCount();
        Meta metaInfo = MetaDelegator.metaGenerator(isEnd, openRecipeAPIPageResult.getTotalPage(), TotalValue.getTotalCount());
    
        return RecipeData.builder()
                .meta(metaInfo)
                .openRecipes(openRecipeAPIPageResult.getDtoList())
                .build();
    }
  
    /*
            레시피를 검색 조건에 따라 조회하는 API로 page, size, order과 Search 객체를 parameter로 지원
            Search는 id, 이름, 조리 방법, 요리 구분, 식재료를 조건으로 설정, and 검색
     */
    @GetMapping(value = "/v1/search/find-only", produces = "application/json; charset=UTF-8")
    public RecipeData responseSearchAndOpenAPI(@RequestParam(defaultValue = DEFAULT_PAGE) int page, @RequestParam(defaultValue = DEFAULT_SIZE) int size, @RequestParam(defaultValue = DEFAULT_ORDER) String order, Search value) {
        Sort sort = order.equals("f") ? Sort.by("favorite").descending() : Sort.by("id").ascending();
        SearchWithPageRequest searchWithPageRequest = searchWithPageHandler.choosePageWithSearch(value, page, size);
    
        // 페이지 설정과 검색 조건을 담은 객체를 전달
        APIPageResult<OpenRecipe, OpenRecipeEntity> openRecipeAPIPageResult = openRecipePageWithSearchService.searchAndAPIDataSources(searchWithPageRequest.getSearch(), searchWithPageRequest.detailOfSort(sort));
    }
  
    /*
            좋아요 레시피를 많은 순으로 상위 8가지를 조회하는 API
     */
    @GetMapping(value = "/v1/recipes/rank", produces = "application/json; charset=UTF-8")
    public RecipeData responseFavoriteOpenAPI() {
        List<OpenRecipe> openRecipes = openRecipePageWithSearchService.mostAndroidRecipe(); // 좋아요를 1개 이상 받은 레시피 중 제일 좋아요가 많은 순서로 최대 8가지 전달
    
        Meta metaInfo = MetaDelegator.metaGenerator(true, openRecipes.size(), 0);
    
        return RecipeData.builder()
                .meta(metaInfo)
                .openRecipes(openRecipes)
                .build();
    }
}
```

#### OpenRecipePageWithSearchService
```java
@Service
public class RecipeAndSearchServiceImpl implements OpenRecipePageWithSearchService {
    @Override
    public APIPageResult<OpenRecipe, OpenRecipeEntity> allAPIDataSources(PageRequest pageRequest) {
        /*
                API page 생성 시 Stream 함수의 map에 function 자리에 들어감
                Functional interface로 entity를 DTO로 바꿔 Json으로 전달하도록 함
         */
        Function<OpenRecipeEntity, OpenRecipe> function = (OpenRecipeConverter::entityToDto);
        Page<OpenRecipeEntity> openRecipeEntities = openRecipePageWithSearchRepository.openAPIPageHandling(pageRequest); // 페이지 처리된 레시피 데이터 리스트
        return new APIPageResult<>(openRecipeEntities, function); // 추가 페이지 정보를 담은 객체로 전달
    }
  
    @Override
    public APIPageResult<OpenRecipe, OpenRecipeEntity> searchAndAPIDataSources(Search search, PageRequest pageRequest) {
        Function<OpenRecipeEntity, OpenRecipe> function = (OpenRecipeConverter::entityToDto);
        
        // 검색 기능을 추가한 DB 조회용 API Search 객체에 검색할 단어를 담아 전달
        Page<OpenRecipeEntity> openRecipeEntities = openRecipePageWithSearchRepository.openAPISearchAndPageHandling(search, pageRequest);
        return new APIPageResult<>(openRecipeEntities, function);
    }
  
    // 인기 레시피 - 하단 메인 화면의 레시피 추천과 같은 함수 사용
    @Override
    public List<OpenRecipe> mostAndroidRecipe() {
        List<OpenRecipe> result = new ArrayList<>();
        List<Object[]> rankFavorites = favoriteRankRepository.findWithRankFavoriteRecipe(); // 제일 좋아요를 많이 받은 순으로 8가지의 레시피를 제공
        
        // 값이 있다면 object[]로부터 값 꺼내 레시피 객체로 변경
        if (!rankFavorites.isEmpty()) {
            result = rankFavorites.stream().map(entity -> OpenRecipeConverter.entityToDto((OpenRecipeEntity) entity[1])).collect(Collectors.toList());
        }
        return result;
    }
}
```

#### OpenRecipePageWithSearchRepository
```java
@Repository
public class RecipeTupleAndPageWithSearchRepositoryImpl extends QuerydslRepositorySupport implements OpenRecipePageWithSearchRepository {
    @PersistenceContext
    private EntityManager entityManager; // 기존의 EntityManager에 필요한 EntityFactory를 사용한 생성 등을 주입
    private final QOpenRecipeEntity openRecipeEntity = QOpenRecipeEntity.openRecipeEntity; // QueryDsl 빌드 시 생성되는 QEntity
  
    public RecipeTupleAndPageWithSearchRepositoryImpl() {
        super(OpenRecipeEntity.class); // QueryDsl 생성자 전달용
    }
  
    // 모든 레시피 조회
    @Override
    public Page<OpenRecipeEntity> openAPIPageHandling(Pageable pageable) {
        /*
                레시피의 좋아요 개수로 정렬하는 경우 OpenRecipeEntity에 해당 필드가 없기 때문에 오류 발생
                NumberPath를 통해 정렬 때 사용할 수 있도록 함
         */
        NumberPath<Long> aliasRecipe = Expressions.numberPath(Long.class, "id");
        JPAQuery<Tuple> openAPIDataHandle = withSelectInit(aliasRecipe); // select시 필드가 여러값이기 때문에 단일 객체가 아닌 tuple로 조회
        
        return pagingWithSortHandler(openAPIDataHandle, aliasRecipe, pageable); // 최종적으로 데이터를 담은 Page 객체 반환
    }
  
    @Override
    public Page<OpenRecipeEntity> openAPISearchAndPageHandling(Search searchKeywords, Pageable pageable) {
        NumberPath<Long> aliasRecipe = Expressions.numberPath(Long.class, "id");
        JPAQuery<Tuple> openAPIDataHandle = withSelectInit(aliasRecipe);
        
        openAPIDataHandle.where(searchAndQueryBuilder(searchKeywords)); // And 검색을 위한 where 절 조회
        return pagingWithSortHandler(openAPIDataHandle, aliasRecipe, pageable);
    }
  
    // left join을 통해 Favorite Recipe DB의 Recipe의 개수를 count
    private JPAQuery<Tuple> withSelectInit(NumberPath<Long> aliasRecipe) {
        return jpaQueryWithCountStart(aliasRecipe).leftJoin(favoriteEntity).on(favoriteEntity.recipe.id.eq(openRecipeEntity.id)).groupBy(openRecipeEntity.id);
    }
  
    // select 문으로 Query entity를 통해 OpenRecipe와 count를 조회
    private JPAQuery<Tuple> jpaQueryWithCountStart(NumberPath<Long> aliasRecipe) {
        // 정렬 기준을 만들기 위해 설정한 NumberPath로 별칭 설정
        return jpaQueryOpenInit().from(openRecipeEntity).select(openRecipeEntity, favoriteEntity.recipe.id.count().as(aliasRecipe));
    }
  
    // entityManager를 통해 JPA Query 생성
    private JPAQuery<OpenRecipeEntity> jpaQueryOpenInit() {
        return new JPAQuery<>(entityManager);
    }
    
    private Page<OpenRecipeEntity> pagingWithSortHandler(JPAQuery<Tuple> query, NumberPath<Long> aliasRecipe, Pageable pageable) {
        totalCountSetting(query.fetch().size());
        pageSortSetting(query, aliasRecipe, pageable.getSort()); // JPAQuery page의 sort를 통해 정렬함
        List<Tuple> tupleResult = sqlTuplePageSetting(query, pageable);
    
        /*
                tuple로 조회된 데이터를 toArray로 변환해 원하는 데이터 값(OpenRecipe 값)을 꺼냄 
                tuple 내 데이터는 select에서 요청한 핃드의 순서와 같음
                0 - openRecipeEntity, 1 - 레시피 좋아요 개수
         */
        List<OpenRecipeEntity> result = tupleResult.stream().map(tuple -> (OpenRecipeEntity) tuple.toArray()[0]).collect(Collectors.toList());
        long count = TotalValue.getTotalCount();
        return new PageImpl<>(result, pageable, count);
    }
  
    private void pageSortSetting(JPQLQuery<Tuple> query, NumberPath<Long> aliasRecipe, Sort pageSort) {
        // QueryDSl에서 기본 정렬은 상세하게 할 수 없기 때문에 구식의 정렬 방식을 사용해 정렬 
        pageSort.stream().forEach(order -> {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String prop = order.getProperty();
            PathBuilder orderByExpression = new PathBuilder(OpenRecipeEntity.class, "openRecipeEntity");
            
            // order로 property, 정렬 필드 값이 무엇인가에 따라 사용하는 기준 필드가 다름
            if (prop.equals("favorite")) {
                query.orderBy(aliasRecipe.desc()); // 좋아요의 개수는 openRecipeEnitiy 필드에 없으므로 numberPath를 통해 정렬
            } else {
                query.orderBy(new OrderSpecifier(direction, orderByExpression.get(prop))); // openRecipeEntity의 객체에 존재하는 값 - id로 정렬 기본 순서
            }
        });
    }
  
    private List<Tuple> sqlTuplePageSetting(JPAQuery<Tuple> openAPIDataHandle, Pageable pageable) {
        openAPIDataHandle.offset(pageable.getOffset()).limit(pageable.getPageSize()); // page 위치와 페이지 당 객체의 개수 설정
        return openAPIDataHandle.fetch();
    }
    
    private BooleanBuilder searchAndQueryBuilder(Search keywords) {
        BooleanBuilder queryResult = defaultBooleanBuilder();
        queryResult.and(detailQuery(keywords.getDetail()) // and 조건문 설정 각 키워드별 함수
                .and(nameQuery(keywords.getName()))
                .and(partQuery(keywords.getPart()))
                .and(wayQuery(keywords.getWay()))
        );
    
        // seq는 키 값이기 때문에 0L보다 커야 검색이 가능, 만일 값이 0L일 때 이 조건 때문에 전체 조건문이 만족되지 않을 수 있기 때문에 설정
        if (keywords.getSeq() > 0L) {
            queryResult.and(seqQuery(keywords.getSeq()));
        }
        return queryResult;
    }
    
    private BooleanBuilder defaultBooleanBuilder() {
        BooleanExpression booleanExpression = openRecipeEntity.id.gt(0L); // gt 함수 : id - 개인키 값이 0L보다 큰 모든 값 조회 데이터 생성 시 1L부터 생성됨
        return new BooleanBuilder().and(booleanExpression);
    }
  
    // 검색어별 and 조건에 boolean expression 생성 함수
    private BooleanExpression nameQuery(String name) {
        return openRecipeEntity.rcpNm.contains(name);
    }
  
    private BooleanExpression detailQuery(String detail) {
        return openRecipeEntity.rcpPartsDtls.contains(detail);
    }
  
    private BooleanExpression partQuery(String part) {
        return openRecipeEntity.rcpPat2.contains(part);
    }
  
    private BooleanExpression wayQuery(String way) {
        return openRecipeEntity.rcpWay2.contains(way);
    }
  
    private BooleanExpression seqQuery(Long seq) {
        return openRecipeEntity.rcpSeq.eq(seq);
    }
}
```

페이지 처리를 위한 객체

#### Abstract page
```java
// Page Result 추상화 객체
@Getter
@Setter
public abstract class PageResult<DTO, EN> {
    private List<DTO> dtoList; // 페이지 당 실질적으로 사용되는 값이 들어가는 list 변수
  
    private int totalPage;
    private int nowPage;
    private boolean firstPage, lastPage;
  
    // 기본 생성자
    public PageResult(Page<EN> result, Function<EN, DTO> fn) {
        this.dtoList = result.stream().map(fn).collect(Collectors.toList()); // DTO -> Entity로 변경해 list로 만듦
        totalPage = result.getTotalPages();
        makePageList(result.getPageable());
    }
    
    public abstract void makePageList(Pageable pageable);
    public abstract void setPage(Pageable pageable);
}
```

#### API Page
```java
// API page 설정
public class APIPageResult<DTO, EN> extends PageResult<DTO, EN> {
      
    /*
            API의 page 처리를 위한 객체
            추상 클래스 PageResult를 구현
    */
  
    public APIPageResult(Page<EN> result, Function<EN, DTO> fn) {
        super(result, fn); // 추상화 객체의 생성자 사용
    }
  
    // 페이지 생성, 설정, 부모 객체의 함수 사용
    @Override
    public void makePageList(Pageable pageable) {
        setPage(pageable);
        boolean isLast = super.getNowPage() == super.getTotalPage();
        super.setLastPage(isLast);
    }
  
    @Override
    public void setPage(Pageable pageable) {
        super.setNowPage(pageable.getPageNumber() + 1);
    }
}
```

#### 조회 API 
모든 레시피를 조회하는 API의 경우 페이지, 정렬 관련 Query만 필요합니다. 검색 조회 API는 Search 객체를 만들어 Query로 받도록 했습니다.

#### 인기 레시피 API
좋아요 레시피를 가져오는 API는 가장 좋아요가 많은 순서로 8가지 레시피를 전송합니다.

#### Open API 저장 테스트용 API
테스트용 API로 공공 Open Recipe API의 모든 데이터를 서버 DB로 저장합니다.

#### 페이지 처리
API에서 기존 공공데이터의 페이지 처리가 없는 문제를 해결하기 위해 해당 데이터를 서버의 DB로 저장하고, 데이터를 페이지 처리해 API로 제공 
기존 방식 : (URL) 시작 인덱스/끝 인덱스 -> 변경된 방식 : (Query Parameter) ?page=1&size=10

#### 기존 공공 데이터에서 개선점
기존 : 시작, 끝 인덱스를 앱에서 계산해서 줘야 하는 문제가 있고, 언제가 끝인지 매번 받아서 확인해야 하는 문제가 있음 - null 확인

개선점 : 페이지 처리가 되어 페이지 요청만 하면 계산할 필요없이 조회가 간편하고 정렬이나 검색 기능을 추가하고 관리할 수 있음



#
### 좋아요 관련 API
  - 좋아요 객체를 JSON으로 전달
  - 쿼리 : recipe id (레시피 식별 번호) (Long), user uid (firebase 유저 uid) (String)
  - give (GET 조회)
    - 모든 좋아요 조회 : `/api/v1/favorites/give`
    - 사용자의 좋아요 조회 : `/api/v1/favorites/give/user`
      - 쿼리 : uid
    - 레시피의 좋아요 조회 : `/api/v1/favorites/give/recipe`
      - 쿼리 : recipe sequence
  - take (POST 좋아요 추가)
    - 유저의 모든 좋아요 리스트 저장 : `/api/v1/favorites/take`
      - 쿼리 : uid
      - request body : recipe sequence list 
    - 유저의 특정 좋아요 저장 : `/api/v1/favorites/take/choose`
      - 쿼리 : uid, recipe sequence
  - delete (POST 좋아요 삭제)
    - 모든 좋아요 삭제 : `/api/v1/favorites/delete`
    - 해당 유저의 모든 좋아요 삭제 : `/api/v1/favorites/delete/user`
      - 쿼리 : uid
    - 유저의 좋아요 삭제 : `/api/v1/favorites/delete/choose`
      - 쿼리 : uid, recipe sequence


### 코드 상세

Firebase 설정 Config

#### Configuration
```java
@Configuration
public class FirebaseConfig {
    private final ApplicationContext applicationContext;
      
    /*
            Firebase 사용을 위한 설정 파일
            Firebase를 사용하기 위해 받은 Json 파일로 인증을 하고 클라우드 DB에 접속
     */
    
    private final ApplicationContext applicationContext;
  
    public FirebaseConfig(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
  
    @PostConstruct
    public void init() {
        firebaseApp();
    }
  
    private void firebaseApp() {
        try {
            // Firebase 기본 설정 - Json 파일, 프로젝트 이름 설정
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(this.firebaseSettingFromJsonFileInput()))
                    .setStorageBucket("ecorecipes-5f00b.appspot.com")
                    .build();
      
            // Firebase 사용
            FirebaseApp.initializeApp(options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 기본적으로 사용되는 파일 읽는 방식으로 로컬 테스트에서 사용할 때는 문제 없음
    private FileInputStream firebaseSettingFromJsonFile() throws FileNotFoundException {
        return new FileInputStream("src/main/resources/ecorecipes-5f00b-firebase-adminsdk-c962f-26f07a6fa6.json");
    }
  
    // 기존의 방식으로는 서버에서 부팅을 위해 jar로 만들 경우 해당 파일을 읽지 못하는 문제 발생
    // 따라서 jar인 상태에도 파일을 가져올 수 있도록 ApplicationContext를 사용해 resources 폴더에서 바로 파일을 가져오도록 설정
    private InputStream firebaseSettingFromJsonFileInput() throws IOException {
        return this.applicationContext.getResource("classpath:ecorecipes-5f00b-firebase-adminsdk-c962f-26f07a6fa6.json").getInputStream();
    }
}
```

Firebase에서 쿼리로 넘어오는 uid로부터 이메일을 얻기 위해 사용

#### Firebase Manager
```java
/*
        Firebase를 사용해 로그인하는 앱, iOS의 사용자의 좋아요 데이터를 저장하기 위한 
 */
@Component
public interface FirebaseUserManager {
    // 해당 사용자가 존재하는지 확인
    boolean isAppUserByUid(String uid);
  
    // 해당 유저의 email 값을 조회
    String findEmailByUid(String uid);
}

public class FirebaseUserManagerImpl implements FirebaseUserManager {
      
    // 사용자 존재 확인
    @Override
    public boolean isAppUserByUid(String uid) {
        try {
            // Authentication 사용 시 유저 데이터를 조회 할 수 있는 클래스 제공
            UserRecord userByUid = FirebaseAuth.getInstance().getUser(uid);
            return !userByUid.getUid().isEmpty();
        } catch (FirebaseAuthException e) {
            log.error("Can't found user value in Firebase", e);
            return false;
        }
    }
  
    // 사용자의 이메일 확인
    @Override
    public String findEmailByUid(String uid) {
        try {
            UserRecord userByUid = FirebaseAuth.getInstance().getUser(uid);
            return userByUid.getEmail();
        } catch (FirebaseAuthException e) {
              log.error("Can't found user value in Firebase", e);
              return "";
        }
    }
}
```

좋아요를 응답하고 받는 API

#### Controller
```java
@RequestMapping("/api/v1/favorites")
@RestController
public class RecipeAPIController {
    private final FavoriteService favoriteService;
    private final FirebaseUserManager firebaseUserManager;
    private final OpenRecipeService openRecipeService;
      
    /*
            모바일 API
            사용자의 좋아요 전달 (GET)
            uid를 받으면 firebase에서 유저의 email을 조회해 서버 DB에 사용자의 좋아요를 찾아 Json으로 응답
     */
    @GetMapping("/give/user")
    public FavoriteData requestUsersFavoriteRecipe(@RequestParam String uid) {
        List<Favorite> findValues = new ArrayList<>();
        
        // firebase에 로그인 했다면, uid에 사용자의 이메일이 있음
        if (firebaseUserManager.isAppUserByUid(uid)) {
            String email = firebaseUserManager.findEmailByUid(uid);
            findValues = favoriteService.findByEmail(email);
        }
        
        // 찾아진 데이터를 간단하게 id와 email만 전달
        List<FavoriteRecipe> favoriteRecipes = favoriteRecipeSerialization(findValues);
    
        return FavoriteData.builder()
                .count(findValues.size())
                .favoriteRecipes(favoriteRecipes)
                .build();
    }
  
    /*
            사용자의 좋아요 요청을 저장 (POST)
            uid와 레시피의 id 값을 받으면 해당 값을 서버 DB에 저장, 값이 있는 경우 저장하지 않음
     */
    @PostMapping("/take/choose")
    public FavoriteData responseOneFavoriteRecipe(@RequestParam String uid, @RequestParam Long recipeSeq) {
        List<Favorite> savedValues = new ArrayList<>();
        
        if (firebaseUserManager.isAppUserByUid(uid)) {
            String email = firebaseUserManager.findEmailByUid(uid);
            OpenRecipe recipe = openRecipeService.findByRecipeSeq(recipeSeq);
            
            // email과 레시피의 id가 있는지 확인
            Favorite favorite = favoriteService.findRecipe(recipeSeq, email);
      
            // 해당 좋아요가 없다면 좋아요를 만듦
            if (favorite.getId().equals(0L)) {
                  Favorite build = Favorite.builder()
                          .recipeId(recipe.getId())
                          .recipeSeq(recipe.getRcpSeq())
                          .userEmail(email)
                          .build();
        
                  favorite = favoriteService.create(build);
            }
            savedValues.add(favorite);
        }
    
        return FavoriteData.builder()
                .count(savedValues.size())
                .favoriteRecipes(favoriteRecipeSerialization(savedValues))
                .build();
    }
    
    // 좋아요 데이터 조회 후 변경
    private List<FavoriteRecipe> favoriteRecipeSerialization(List<Favorite> values) {
        return values.stream().map(this::serialization).collect(Collectors.toList());
    }
  
    private FavoriteRecipe serialization(Favorite favorite) {
        return FavoriteRecipe.builder()
                .id(favorite.getId())
                .recipeId(favorite.getRecipeId())
                .recipeSeq(favorite.getRecipeSeq())
                .userEmail(favorite.getUserEmail())
                .build();
    }
}
```

#### Service
```java
@RequiredArgsConstructor
@Service
public class RecipeAndSearchServiceImpl implements OpenRecipeService {
    @Override
    public OpenRecipe findByRecipeSeq(Long recipeSeq) {
        // 데이터를 가져오는 JPA 사용
        Optional<OpenRecipeEntity> findOpenRecipe = openRecipeRepository.findByRcpSeq(recipeSeq);
        // Java의 map을 활용해 entity를 dto로 변경하고 불러온 값에 데이터가 없다면 빈 dto 반환
        return findOpenRecipe.map(OpenRecipeConverter::entityToDto).orElse(OpenRecipe.builder().build());
    }
}

// 좋아요 Service Interface
public interface FavoriteService {
    // entity에서 dto로 변경하는 함수
    default Favorite entityToDto(FavoriteEntity favoriteEntity) {
        // 아래의 함수와 OpenRecipe로부터 값을 얻는것 외엔 일치하기 때문에 생략
    }
  
    default Favorite entityToDto(FavoriteEntity favoriteEntity, OpenRecipeEntity openRecipeEntity) {
        return Favorite.builder()
                .id(favoriteEntity.getId())
                .recipeId(openRecipeEntity.getId())
                .recipeSeq(openRecipeEntity.getRcpSeq())
                .recipeMainImage(openRecipeEntity.getAttFileNoMain())
                .recipeName(openRecipeEntity.getRcpNm())
                .recipePart(openRecipeEntity.getRcpPat2())
                .recipeWay(openRecipeEntity.getRcpWay2())
                .userEmail(favoriteEntity.getUserEmail())
                .build();
    }
  
    // dto에서 entity로 변경하는 함수
    default FavoriteEntity dtoToEntity(Favorite favorite) {
        OpenRecipeEntity recipe = OpenRecipeEntity.builder()
                .id(favorite.getRecipeId())
                .rcpSeq(favorite.getRecipeSeq())
                .attFileNoMain(favorite.getRecipeMainImage())
                .rcpNm(favorite.getRecipeName())
                .rcpWay2(favorite.getRecipeWay())
                .rcpPat2(favorite.getRecipePart())
                .build();
    
        return FavoriteEntity.builder()
                .id(favorite.getId())
                .recipe(recipe)
                .userEmail(favorite.getUserEmail())
                .build();
    }
}

// 좋아요 Service 구현체
@RequiredArgsConstructor
@Service
public class FavoriteServiceImpl implements FavoriteService {
  
    /*
            좋아요 생성 시 이미 같은 회원이 같은 레시피로 좋아요가 되어있는지 확인
     */
    @Override
    public Favorite create(Favorite favorite) {
        FavoriteEntity favoriteEntity = dtoToEntity(favorite);
        
        // 존재 여부 확인
        if (!isExist(favorite.getRecipeId(), favorite.getUserEmail())) {
            // JPA 기본 생성 함수
            FavoriteEntity savedFavoriteEntity = favoriteSimpleRepository.save(favoriteEntity);
            return entityToDto(savedFavoriteEntity);
        }
        return Favorite.builder().build();
    }
  
    // 만약 조회가 될 경우 값이 반환되므로 null이 아닌 값이 전달 true
    private boolean isExist(Long recipeSeq, String email) {
        return favoriteRepository.findFavoriteByRecipeSeqAndEmail(recipeSeq, email) != null;
    }
      
    /*
            이메일과 레시피의 id로 좋아요 조회
     */
    @Override
    public Favorite findRecipe(Long recipeSeq, String email) {
        // 좋아요 없는 경우 id 0L만 넣은 빈 객체 전달
        Favorite favorite = Favorite.builder().id(0L).build();
    
        // Querydsl을 사용해 tuple로 값을 가져왔기 때문에 배열로 변경해 사용
        Object[] foundRawValue = favoriteRepository.findFavoriteByRecipeSeqAndEmail(recipeSeq, email);
        
        if (foundRawValue != null) {
            // Object[]는 각자 select 문에서 조회하기로 한 필드를 순서대로 담고 있다.
            FavoriteEntity favoriteEntity = (FavoriteEntity) foundRawValue[0];
            OpenRecipeEntity openRecipeEntity = (OpenRecipeEntity) foundRawValue[1];
            favorite = entityToDto(favoriteEntity, openRecipeEntity);
        }
        return favorite;
    }
  
    /*
            이메일을 통해 해당 유저의 모든 좋아요 조회
     */
    @Override
    public List<Favorite> findByEmail(String email) {
        List<Object[]> allFavorite = favoriteRepository.findFavoriteByEmail(email);
        return valueNotFoundCheck(allFavorite);
    }
  
    // 값이 찾아졌는지 확인하는 함수
    private List<Favorite> valueNotFoundCheck(List<Object[]> found) {
        List<Favorite> favoriteList = new ArrayList<>();
    
        if (found != null) {
            for (Object[] values : found) {
                Favorite f = entityToDto((FavoriteEntity) values[0], (OpenRecipeEntity) values[1]);
                f.setCount((Long) values[2]);
                favoriteList.add(f);
            }
        } else {
            // 없다면 id 0L만 넣은채 반환
            favoriteList.add(Favorite.builder().id(0L).build());
        }
        return favoriteList;
    }
}
```

#### Repository
```java
// 레시피 Repository 구현체
@Repository
public class FavoriteRepositoryImpl extends QuerydslRepositorySupport implements FavoriteRepository, FavoriteRankRepository {
    // QueryDSL 생성 객체
    private final QFavoriteEntity favoriteEntity = QFavoriteEntity.favoriteEntity;
    private final QOpenRecipeEntity openRecipeEntity = QOpenRecipeEntity.openRecipeEntity;
  
    @Override
    public List<Object[]> findFavoriteByEmail(String email) {
        // JPQLQuery로 select 구문 생성
        JPQLQuery<Tuple> selectFavoriteRecipe = selectFavoriteRecipe(from(favoriteEntity));
        
        // 이메일로 검색 조건 설정, 이메일이 완전 같을 경우
        selectFavoriteRecipe.where(favoriteEntity.userEmail.eq(email));
        return selectFavoriteRecipe.fetch().stream().map(Tuple::toArray).collect(Collectors.toList());
    }
  
    @Override
    public Object[] findFavoriteByRecipeSeqAndEmail(Long recipeSeq, String email) {
        JPQLQuery<Tuple> selectFavoriteRecipe = selectFavoriteRecipe(from(favoriteEntity));
        selectFavoriteRecipe.where(favoriteEntity.recipe.rcpSeq.eq(recipeSeq).and(favoriteEntity.userEmail.eq(email)));
        
        // 일치하는 1개의 조회 결과 반환
        Tuple tuple = selectFavoriteRecipe.fetchFirst();
    
        // tuple에는 2개의 결과가 Object로 존재 - favoriteEntity, openRecipeEntity
        if (tuple != null) {
            return tuple.toArray();
        }
        return null;
    }
  
    // 좋아요에서 count를 위해 left join으로 레시피를 조인 - 1 : n
    private JPQLQuery<Tuple> selectFavoriteRecipe(JPQLQuery<FavoriteEntity> query) {
        // 좋아요와 레시피 모두를 조회
        return query.select(favoriteEntity, openRecipeEntity)
                .leftJoin(openRecipeEntity).on(favoriteEntity.recipe.id.eq(openRecipeEntity.id))
                .from(favoriteEntity);
    }
}
```

#### 좋아요 API
좋아요 조회, 추가, 삭제 관련 기능을 앱에 제공하는 API입니다. 조회는 모든 좋아요 보기, 이메일에 따른 조회, 레시피에 따른 조회가 있고
추가는 이메일 하나에 여러 레시피 목록을 통해 한번에 여러 개의 좋아요를 추가하거나 이메일과 레시피 하나를 추가하는 기능이 있습니다.
삭제는 해당 이메일의 모든 좋아요 삭제와 이메일과 레시피가 같은 좋아요 삭제가 있습니다.

#### Firebase 로그인 확인
앱에서는 Firebase Authentication에서 지원하는 로그인 방식을 통해 로그인 하는데, 이때 로그인한 사용자의 정보가 Firebase User
클라우드 데이터베이스에 저장됩니다. 앱에서 Uid를 사용해 사용자의 좋아요를 처리하는데 이 Uid를 받아 이메일을 조회해 사용자의 이메일과
레시피를 서버의 좋아요 (Favorite Recipe) DB에 저장합니다.


## 사이트 구현

### 메인화면
![](recipe/img/레시피_메인상단.png)
<div style="text-align: center;">메인화면 상단</div>

#### 상단 메뉴바 

#### header
```html
<html lang="en" xmlns:th="http://www.thymeleaf.org">

<!--thymeleaf에서 해당 태그를 붙이고 replace 등 대체 태그를 사용하면 중복 html 코드를 줄일 수 있다.-->
<th:block th:fragment="header"> 

    <nav class="navbar navbar-color-on-scroll navbar-transparent fixed-top  navbar-expand-lg" color-on-scroll="100" id="sectionsNav">
        <div class="container">
            ... 중략 ...
            <div class="collapse navbar-collapse">
                <ul class="navbar-nav ml-auto">
                    <!-- 지정한 데이터 (user의 값)이 없는 경우 로그인 버튼, 로그인이 되어 있는 경우 로그아웃 버튼-->
                    <li class="button-container nav-item iframe-extern" th:if="${ user == null }">
                        <button class="btn btn-sm btn-block btn-round" data-toggle="modal" data-target="#loginModal">
                            로그인
                        </button>
                    </li>
                    <li class="button-container nav-item iframe-extern" th:unless="${ user == null }">
                        <a href="#" th:href="@{/logout}" class="btn btn-sm btn-round btn-block">
                            로그아웃
                        </a>
                    </li>
                  
                </ul>
            </div>
        </div>
    </nav>

    <!--  로그인창 모달-->
    <div class="modal fade" id="loginModal" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-login" role="document">
            <div class="modal-content">
                <div class="card card-signup">
                    <div class="modal-header">
                        <div class="card-header card-header-success text-center">
                            <button type="button" class="close" data-dismiss="modal" aria-hidden="true"><i class="material-icons">clear</i></button>
                            <h4 class="card-title">로그인</h4>
                            <div class="social-line">
                                <p>
                                    구글로 로그인하기
                                </p>
                            </div>
                        </div>
                    </div>
                    <!-- 실질적으로 로그인하는 구글 링크 -->
                    <div class="modal-body">
                        <div class="card-body">
                            <a href="index.html" th:href="@{http://www.inndiary.xyz/oauth2/authorization/google}" id="google" class="btn btn-google btn-round">
                                <i class="fa fa-google-plus"></i> Google
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

</th:block>

<!--사용 시-->
<div th:replace="~{ header :: header }"></div>
```

#### 사이트 상단 메뉴
사이트 상단의 메뉴로 동일 코드를 header 파일로 replace 해 사용
하단 footer 도 replace로 대체


#### 메인 페이지 추천 레시피

![](recipe/img/레시피_메인_추천레시피.png)
<div style="text-align: center;">메인화면 상단 추천 레시피</div>

![](recipe/img/레시피_메인_관련레시피.png)
<div style="text-align: center;">메인화면 하단 관련 레시피</div>

#### index
```html

<div class="container">
  <div class="title">
      <h3 id="manualRotatingCards">추천 요리 레시피</h3>
  </div>
  
  <!-- 좋아요 많은 레시피가 4개 이하인 경우 - 좋아요를 1개 이상 받은 경우만 -->
  <th:block th:if="${ #lists.size(rank_recipes) > 0 }">
      <div class="row display-4">
        
        <!-- 0번 배열에 첫번째 줄 좋아요 많은 순서로 보여짐 -->
          <div class="col-md-3 col-lg-3" th:each="recipe : ${ rank_recipes[0] }" th:object="${ recipe }">
              <div class="rotating-card-container manual-flip">
                  <div class="card card-rotate">
                      <div th:if="*{ recipeMainImage == null }" class="front front-background" style="background-image: url('../static/assets/img/image_placeholder.jpg');" th:style="'background-image: url(/assets/img/image_placeholder.jpg);'">
                          <div class="card-body">
                              <h5 class="card-category card-category-social text-success">
                                  [[ *{ recipeName } ]]
                              </h5>
                              <div class="stats text-center">
                                  <button type="button" name="button" class="btn btn-success btn-fill btn-round btn-rotate">
                                      <i class="material-icons">refresh</i> 자세히
                                  </button>
                              </div>
                          </div>
                      </div>
                      <div th:unless="*{ recipeMainImage == null }" class="front front-background" style="background-image: url('../static/assets/img/image_placeholder.jpg');" th:style="'background-image: url(' + *{ recipeMainImage } + ');'">
                          <div class="card-body">
                              <h5 class="card-category card-category-social text-success">
                                  [[ *{ recipeName } ]]
                              </h5>
                              <div class="stats text-center">
                                  <button type="button" name="button" class="btn btn-success btn-fill btn-round btn-rotate">
                                      <i class="material-icons">refresh</i> 자세히
                                  </button>
                              </div>
                          </div>
                      </div>
                      <div class="back">
                          <div class="card-body">
                              <button type="button" class="btn btn-sm btn-light btn-link btn-rose" style="pointer-events: none;" th:style="'pointer-events: none;'">
                                  <i class="material-icons">favorite</i>
                              </button>
                              <p>[[ *{ count } ]]</p>
                              <h5 class="card-title">
                                  [[ *{ recipeName } ]]
                              </h5>
                              <p class="card-description">
                                  조리방식 : [[ *{ recipeWay } ]]
                              </p>
                              <p class="card-description">
                                  조리분류 : [[ *{ recipePart } ]]
                              </p>
                              <br>
                              <a type="button" th:href="@{'/recipes/detail/' + *{ recipeId }}">
                                  <i class="material-icons">subject</i> 자세히보기
                              </a>
                              <button type="button" name="button" class="btn btn-link btn-round btn-rotate">
                                  <i class="material-icons">refresh</i> 돌아가기
                              </button>
                          </div>
                      </div>
                  </div>
              </div>
          </div>
        
      </div>
    
      <!-- 좋아요 많은 순 (1개 이상의 좋아요) 레시피가 4개 이상인 경우, 최대 8개까지 보여줌 -->
      <div class="row display-4" th:if="${ #lists.size(rank_recipes) >= 2 }">
        
          <div class="col-md-3 col-lg-3" th:each="recipe : ${ rank_recipes[1] }" th:object="${ recipe }">
              ... 위의 코드와 동일 ...
          </div>
        
      </div>
  </th:block>

  <!-- 만일 좋아요를 받은 레시피가 전혀 없을 경우 -->
  <th:block th:unless="${ #lists.size(rank_recipes) > 0 }">
      <br>
      <div class="row">
          <div>
              아직 좋아요한
              레시피가 없습니다.
          </div>
      </div>
  </th:block>
</div>
```

#### 추천 레시피
좋아요를 받은 레시피 중 좋아요가 제일 많은 순서로 8개의 레시피를 추천합니다. 8개보다 적을 경우 좋아요를 받은 레시피만 추천됩니다.

#### 추천 관련 레시피
추천 레시피와 관련 있는 레시피로 좋아요가 제일 많은 첫번째 레시피와 조리, 요리 분류와 관련된 레시피를 무작위로 4가지 추천합니다.
만약 좋아요한 레시피가 없다면 굽기, 반찬을 기본으로 추천합니다.

#### Controller
```java
// 사이트 접속 시 가장 먼저 보게 되는 메인 화면
@RequestMapping("/")
@Controller
@RequiredArgsConstructor
public class HomeController {
    private final FavoriteRankService favoriteRankService;
    private final RecipeRecommendService recipeRecommendService;
    private final SearchWithPageHandler<Favorite> searchWithPageHandler;

    @GetMapping
    public String homePage(Model model, @LoginSession SessionUser user) {
        // 좋아요가 많은 순서대로 제공
        List<Favorite> favorites = favoriteRankService.mostFavoriteRecipe();
        List<List<Favorite>> rankRecipe = new ArrayList<>();
        
        if (user != null) {
            // 로그인이 되어 있다면 user 데이터 제공
            model.addAttribute("user", user);
        }

        // 기본 추천 레시피 카테고리
        String way = "굽기";
        String part = "반찬";
        Search waySearch = Search.builder().way("굽기").build();
        Search partSearch = Search.builder().part("반찬").build();
        
        // 좋아요 숫자에 따라 전달되는 값이 다름
        if (favorites.size() > 0) {
            // page Handler에서 2줄이 넘어가는 경우 2차원 배열로 반환
            rankRecipe = searchWithPageHandler.pageRowRank(favorites);

            // 레시피가 1개 이상, 레시피의 1번째 (가장 많은 좋아요) 음식 분류, 조리 방식을 설정
            Favorite bestFavorite = favorites.get(0);
            way = bestFavorite.getRecipeWay();
            part = bestFavorite.getRecipePart();
            waySearch = waySearch(way);
            partSearch = partSearch(part);
        }

        // 카테고리에 맞게 4가지 무작위 레시피를 가져옴
        List<Recommend> wayRecommend = recommendRecipeList(waySearch);
        List<Recommend> partRecommend = recommendRecipeList(partSearch);

        model.addAttribute("way", way);
        model.addAttribute("part", part);
        model.addAttribute("rank_recipes", rankRecipe);
        model.addAttribute("wayList", wayRecommend);
        model.addAttribute("partList", partRecommend);
        return "index";
    }

    private List<Recommend> recommendRecipeList(Search search) {
        return recipeRecommendService.findRecommendRecipe(search);
    }

    private Search waySearch(String way) {
        return Search.builder().way(way).build();
    }

    private Search partSearch(String part) {
        return Search.builder().part(part).build();
    }
}
```

#### 레시피 8개 조회 2줄로 변경 코드
```java
// searchWithPage interface로 way와 part를 위해 generator로 생성
// generator로 생성한 이유는 2줄로 변경하는 코드가 좋아요 뿐만 아니라 레시피에도 적용되기 때문이다.
public interface SearchWithPageHandler<T> {
    List<List<T>> pageRowRank(List<T> list);
}

@Component
public class SearchWithPageHandlerImpl<T> implements SearchWithPageHandler<T> {
    
    // 좋아요 레시피 8개의 개수에 따라 row 설정 - 3차원 배열
    @Override
    public List<List<T>> pageRowRank(List<T> list) {
        List<List<T>> rowList = new ArrayList<>();
    
        int start = 0;
        int subIn = 4;
        int midIndex = start + subIn;
        int recipesSize = list.size();
    
        // 8로 제한이 아닌 while문으로 구현한 이유는 4개씩 더 많이 보여주는 다른 페이지 처리에서도 활용하는 함수
        while (true) {
            if (midIndex >= recipesSize) {
                midIndex = recipesSize;
                rowList.add(list.subList(start, midIndex));
                break;
            } else {
                rowList.add(list.subList(start, midIndex));
                start += subIn;
                midIndex += subIn;
            }
        }
        return rowList;
    }
}
```

#### Service
```java
// 좋아요 순위 Service 구현체
@RequiredArgsConstructor
@Service
public class FavoriteServiceImpl implements FavoriteRankService {
    private final FavoriteRankRepository favoriteRankRepository;
  
    @Override
    public List<Favorite> mostFavoriteRecipe() {
        List<Object[]> allFavorite = favoriteRankRepository.findWithRankFavoriteRecipe();
        
        // 값이 정상적으로 존재하는지 확인, 값을 Favorite 객체에 넣음 - 모든 레시피의 필드를 요구하지 않음
        return valueNotFoundCheck(allFavorite);
    }
}

// 레시피 추천 Service 구현체
@Service
@RequiredArgsConstructor
public class RecipeAndSearchServiceImpl implements RecipeRecommendService {
    private final RecipeTupleRepository recipeTupleRepository;
  
    @Override
    public List<Recommend> findRecommendRecipe(Search search) {
        List<Object[]> objects = recipeTupleRepository.sameRecommendRecipe(search);
        return objects.stream().map(objs -> Recommend.builder().id((Long) objs[0]).image((String) objs[1]).name((String) objs[2]).build()).collect(Collectors.toList());
    }
}
```

#### Repository
```java
// 좋아요 순위 Repository 구현체
@Repository
public class FavoriteRepositoryImpl extends QuerydslRepositorySupport implements FavoriteRankRepository {
    private final QFavoriteEntity favoriteEntity = QFavoriteEntity.favoriteEntity;
    private final QOpenRecipeEntity openRecipeEntity = QOpenRecipeEntity.openRecipeEntity;
  
    // 좋아요 많은 순 상위 8가지를 조회
    @Override
    public List<Object[]> findWithRankFavoriteRecipe() {
        JPQLQuery<Tuple> selectFavoriteRecipe = selectFavoriteRecipeWithRank(from(favoriteEntity));
        return orderFavoriteRecipe(selectFavoriteRecipe).fetch().stream().map(Tuple::toArray).collect(Collectors.toList());
    }
  
    private JPQLQuery<Tuple> selectFavoriteRecipeWithRank(JPQLQuery<FavoriteEntity> query) {
        // 좋아요의 순위를 정하기 위해 좋아요 레시피의 개수를 셈. 이메일이 다른 같은 레시피의 여러 좋아요가 있음
        // count 함수를 통해 count 쿼리 구문 사용
        return query.select(favoriteEntity, openRecipeEntity, favoriteEntity.recipe.count())
                .leftJoin(openRecipeEntity).on(favoriteEntity.recipe.id.eq(openRecipeEntity.id))
                .from(favoriteEntity);
    }
  
    private JPQLQuery<Tuple> orderFavoriteRecipe(JPQLQuery<Tuple> selectQuery) {
        // 8가지로 제한
        int rankTotal = 8;
        // 레시피에서 서로 다른 레시피를 구분하기 좋은 필드는 id와 rcpSeq가 있음. 그중 rcpSeq로 group by
        // 정렬은 count가 제일 많은 순서
        return selectQuery.groupBy(favoriteEntity.recipe.rcpSeq).orderBy(favoriteEntity.recipe.id.count().desc()).limit(rankTotal);
    }
}

// 레시피 추천 Repository 구현체
@Repository
public class RecipeTupleAndPageWithSearchRepositoryImpl extends QuerydslRepositorySupport implements RecipeTupleRepository {
    @PersistenceContext
    private EntityManager entityManager;
  
    private final QOpenRecipeEntity openRecipeEntity = QOpenRecipeEntity.openRecipeEntity;
  
    // 키워드와 같은 카테고리를 가진 레시피 중 4가지를 무작위로 조회
    @Override
    public List<Object[]> sameRecommendRecipe(Search search) {
        JPAQuery<Tuple> recipeRecommend = jpaQuerySelectRecommendRandInit();
        recipeRecommend.where(searchAndQueryBuilder(search));
    
        // 무작위 4가지를 가져오기 위해 사용
        recipeRecommend.orderBy(NumberExpression.random().asc()).limit(4);
        List<Tuple> fetch = recipeRecommend.fetch();
        return fetch.stream().map(Tuple::toArray).collect(Collectors.toList());
    }
  
    private JPAQuery<Tuple> jpaQuerySelectRecommendRandInit() {
        // 조회 시 모든 레시피의 데이터는 필요하지 않기 때문에, 구분할 수 있는 id, 사진, 이름만 조회
        return jpaQueryMySqlTemplateInit().from(openRecipeEntity).select(openRecipeEntity.id, openRecipeEntity.attFileNoMain, openRecipeEntity.rcpNm);
    }
  
    // rand 함수를 사용하기 위해 커스텀한 JPQLTemplates로 설정
    private JPAQuery<OpenRecipeEntity> jpaQueryMySqlTemplateInit() {
        return new JPAQuery<>(entityManager, MySqlJpaTemplates.DEFAULT);
    }
}
```

JPA와 MySql은 rand 함수가 제대로 지원되지 않아 적용하지 못하는 부분이 존재한다. 그렇기 때문에 해당 설정을 해주어야 제대로 랜덤으로 값이 나온다.
다른 방법으로는 매번 조회 후 무작위로 값을 정해주는 방식인데, 값이 많아질 수록 너무 오래 걸린다.

#### MySql용 추가 설정
```java
public class MySqlJpaTemplates extends JPQLTemplates {
    
    /*
            기존의 JPQLTemplates에서 지원하는 random() 함수는 MySql에서는 rand() 함수로 지원
            JPQLTemplates에서 rand를 사용하도록 부분만 임의로 함수 수정
     */
    public static final MySqlJpaTemplates DEFAULT = new MySqlJpaTemplates();
    
    public MySqlJpaTemplates() {
        this(DEFAULT_ESCAPE);
        add(Ops.MathOps.RANDOM, "rand()");
        add(Ops.MathOps.RANDOM2, "rand({0})");
    }
    
    public MySqlJpaTemplates(char escape) {
        super(escape);
    }
}
```

### 레시피 목록
![](recipe/img/레시피_보기상단.png)
<div style="text-align: center;">레시피 보기 상단</div>

![](recipe/img/레시피_보기_카테고리.png)
<div style="text-align: center;">레시피 보기 카테고리</div>

![](recipe/img/레시피_보기_모든레시피.png)
<div style="text-align: center;">레시피 보기 모든 목록</div>

#### recipeList.html
```html
<!-- 레시피 카테고리 검색 코드 -->
<!-- form 태그의 th:method를 활용해 get 요청을 진행 action의 URL로 요청 - () 안의 내용은 query를 통해 검색이나 페이지의 내용을 설정함 -->
<form class="ml-auto" th:action="@{/recipes(page=${ page }, size=${ pageCall.size }, name=${ search.name }, detail=${ search.detail}, way=${ search.way }, part=${ search.part })}" th:method="get">
  <input type="hidden" name="size" value="28">
  
  <!-- 레시피 개수를 나타냄 -->
  <h3 class="title text-center">총 [[ ${ recipeTotal } ]]개의 레시피</h3>
  <div id="collapse">
      <div class="container">
        
                            <!-- 일부 코드 파일 확인 -->
                                                        
                        <!-- DB에서 모든 레시피의 조리 방식을 group by해 가져와 선택가능하게 보여줌 -->
                        <div class="form-check" th:each="way, wayStat : ${ ways }">
                            <label class="form-check-label">
                                <input class="form-check-input" type="radio" th:field="*{search.way}" name="way" th:value="${ way.value }">
                                <span class="circle">
                                <span class="check"></span>
                            </span>
                                [[ ${ way.value } ]]
                            </label>
                        </div>
                    </div>
                </div>
                <div class="ripple-container"></div>
            </div>
            <div class="row">
                <h5 class="title">음식 분류</h5>
                <div class="form-inline ml-auto">
                    <div class="form-group bmd-form-group">

                        <!-- DB에서 모든 레시피의 요리 분류를 group by해 가져와 선택가능하게 보여줌 -->
                        <div class="form-check" th:each="part, partStat : ${ parts }">
                            <label class="form-check-label">
                                <input type="radio" th:field="*{search.part}" name="part" th:value="${ part.value }" class="form-check-input">
                                <span class="circle">
                                <span class="check"></span>
                            </span>
                                [[ ${ part.value } ]]
                            </label>
                        </div>
                    </div>
                </div>
                <div class="ripple-container"></div>
            </div>
        </div>
    </div>
    <button type="submit" class="btn btn-success">
        검색
    </button>
    <!-- 정렬 기준 정하기 -->
    <div class="col-lg-5 col-md-6 col-sm-12">
        <select class="selectpicker" data-style="select-with-transition" title="Single Select" name="order" data-size="2">
            <option value="d" th:selected="${ pageCall.order == 'd' }">등록순</option>
            <option value="f" th:selected="${ pageCall.order == 'f' }">인기순</option>
        </select>
    </div>
  
<!-- 생략 -->

<!-- 레시피 목록 코드 -->
<th:block th:if="${ #lists.size(recipeList.divideList) > 0 }">
  <div class="row display-4" th:each="rowList : ${ recipeList.divideList }">
    <div class="col-md-3 col-lg-3" th:each="recipe : ${ rowList }">
      <div class="card card-blog">
        
        <!-- 좋아요와 개수 표시 -->
        <div class="row col-lg-12 ml-auto mr-auto">
          <!-- 좋아요를 표시한 사용자가 로그인한 경우 해당 이메일에 맞는 좋아요가 있는지 확인 -->
          <button type="button" class="btn btn-sm btn-light btn-link btn-rose" style="pointer-events: none;" th:style="'pointer-events: none;'" th:if="${ #arrays.contains(favoriteSeqList, recipe.recipeId) }">
            <i class="material-icons">favorite</i>
          </button>
          <button type="button" class="btn btn-sm btn-light btn-link" style="pointer-events: none;" th:style="'pointer-events: none;'" th:unless="${ #arrays.contains(favoriteSeqList, recipe.recipeId) }">
            <i class="material-icons">favorite</i>
          </button>
          <!-- 해당 레시피의 좋아요 개수 -->
          <h4>[[ ${ recipe.count } ]]</h4>
        </div>
        <div th:if="${ recipe.recipeMainImage != '' }" class="front front-background" style="background-image: url('../../static/assets/img/image_placeholder.jpg')" th:style="'background-image: url(' + ${ recipe.recipeMainImage } + ');'">
          <a href="#" th:href="@{'/recipes/detail/' + ${ recipe.recipeId }}">
            <div class="card-body">
            </div>
          </a>
        </div>
        <div th:unless="${ recipe.recipeMainImage != '' }" class="front front-background" style="background-image: url('../../static/assets/img/image_placeholder.jpg')" th:style="'background-image: url(/assets/img/image_placeholder.jpg);'">
          <a href="#" th:href="@{'/recipes/detail/' + ${ recipe.recipeId }}">
            <div class="card-body">
            </div>
          </a>
        </div>
        <div class="card-body">
          <h6 class="card-category text-info">[[ ${ recipe.recipePart } ]]</h6>
          <h4 class="card-title">
            <a href="#" th:href="@{'/recipes/detail/' + ${ recipe.id }}">[[ ${ recipe.recipeName } ]]</a>
          </h4>
        </div>
      </div>
    </div>
  </div>
</th:block>
<th:block th:unless="${ #lists.size(recipeList.divideList) > 0 }">
  <div class="row">
    <div class="container">
      <h4 class="content-center">검색결과가 없습니다</h4>
    </div>
  </div>
</th:block>

<!-- 페이지 처리 코드 -->
<div class="subscribe-line">
  <div class="pagination-area">
    <ul class="pagination justify-content-center text-center">
      <!-- 쿼리를 유지하기 위해 괄호 안에 각 변수를 적음 - 괄호 안의 내용은 쿼리 파라메터로 전달됨 -->
      <li class="page-item" th:if="${ recipeList.firstPage }">
        <a href="recipeList.html" th:href="@{/recipes(page=1, size=${ pageCall.size }, name=${ search.name }, detail=${ search.detail}, way=${ search.way }, part=${ search.part })}" class="page-link">&#xAB;&#xAB;</a>
      </li>
      <li class="page-item" th:if="${ recipeList.prevPage }">
        <a href="recipeList.html" th:href="@{/recipes(page=${ recipeList.startPage - 1 }, size=${ pageCall.size }, name=${ search.name }, detail=${ search.detail}, way=${ search.way }, part=${ search.part })}" class="page-link">&#xAB;</a>
      </li>

      <!-- 매번 페이지가 선택될 경우 선택된 페이지를 나타냄 (현재 페이지) -->
      <li th:class="'page-item' + ${ recipeList.nowPage == page ? ' active' : '' }" th:each="page : ${ recipeList.pageList }">
        <a href="recipeList.html" th:href="@{/recipes(page=${ page }, size=${ pageCall.size }, name=${ search.name }, detail=${ search.detail}, way=${ search.way }, part=${ search.part })}" class="page-link">[[ ${ page } ]]</a>
      </li>

      <li class="page-item" th:if="${ recipeList.nextPage }">
        <a href="recipeList.html" th:href="@{/recipes(page=${ recipeList.endPage + 1 }, size=${ pageCall.size }, name=${ search.name }, detail=${ search.detail}, way=${ search.way }, part=${ search.part })}" class="page-link">&#xBB;</a>
      </li>
      <li class="page-item" th:if="${ recipeList.lastPage }">
        <a href="recipeList.html" th:href="@{/recipes(page=${ recipeList.totalPage }, size=${ pageCall.size }, name=${ search.name }, detail=${ search.detail}, way=${ search.way }, part=${ search.part })}" class="page-link">&#xBB;&#xBB;</a>
      </li>
    </ul>
  </div>
</div>
```

#### 레시피 카테고리 검색과 목록, 페이지 처리 코드
카테고리를 통해 원하는 레시피를 검색할 수 있습니다. 레시피 이름부터 들어가는 재료의 이름, 음식 분류, 조리 과정을 설정해 검색할 수 있고,
인기순, 기본순으로 정렬할 수 있습니다.

가장 위에는 레시피의 개수를 표시하고 레시피는 최대 4개씩 6줄을 나타냅니다. 페이지는 총 개수에 따라 최대 10페이지 씩 나오며 << 와 >> 를
통해 제일 첫번째 페이지와 제일 끝 페이지로 이동하고 < 와 > 를 통해 이전, 다음페이지로 이동합니다.

![](recipe/img/레시피_보기_모든레시피_마지막.png)
<div style="text-align: center;">레시피 보기 마지막 페이지</div>

### 검색한 결과

![](recipe/img/레시피_보기_검색레시피_결과2.png)
<div style="text-align: center;">카테고리 검색 설정 후 검색 결과</div>

![](recipe/img/레시피_보기_검색레시피_결과.png)
<div style="text-align: center;">레시피 검색 결과</div>

![](recipe/img/레시피_보기_검색레시피_없을시.png)
<div style="text-align: center;">레시피 검색 결과 없을 경우</div>

#### Controller
```java
@RequestMapping("/recipes")
@RequiredArgsConstructor
@Controller
public class RecipeController {
    private final OpenRecipePageWithSearchService openRecipePageWithSearchService;
    private final RecipeService recipeService;
    private final FavoriteRankService favoriteRankService;
    private final SearchWithPageHandler<OpenRecipe> searchWithPageHandler;
  
    // 사용자와 페이지 요청 시 받아들이는 객체, 검색어 객체, 데이터를 전달할 model 객체
    @GetMapping
    public String showAllRecipes(@LoginSession SessionUser user, PageCall pageCall, Search search, Model model) {
        List<Way> ways = recipeService.recipeWayValueFound();
        List<Part> parts = recipeService.recipePartValueFound();
        // 레시피의 조리 방식과 요리 분류가 DB에서 조회됨, DB에서 조회될 때 All 값은 없으므로 추가
        ways.add(0, new Way("All"));
        parts.add(0, new Part("All"));
    
        // 정렬의 기준을 위해 f - 좋아요 많은 순서 d - id, 즉 sequence number 순서
        Sort sort = pageCall.getOrder().equals("f") ? Sort.by("favorite").descending() : Sort.by("id").ascending();
        // All 일경우 "" 빈값을 통해 and 검색을 하지 않음 표시
        Search value = Search.builder().name(search.getName()).seq(search.getSeq()).detail(search.getDetail()).part(search.getPart().equals("All") ? "" : search.getPart()).way(search.getWay().equals("All") ? "" : search.getWay()).build();
        SearchWithPageRequest searchWithPageRequest = searchWithPageHandler.choosePageWithSearch(search, pageCall.getPage(), pageCall.getSize());
        RecipePageResult<Favorite, Object[]> recipePageResult = openRecipePageWithSearchService.searchTuplePageWithSortRecipes(value, searchWithPageHandler.searchPageWithSort(searchWithPageRequest, sort));
        
        List<Long> favoriteSeq = new ArrayList<>();
        if (user != null) {
            // 만일 로그인 되어 있을 시 사용자가 좋아요한 모든 값을 가져와 표시함. 
            favoriteSeq = favoriteRankService.usersFavoriteOnlySeq(user.getEmail());
        }
    
        model.addAttribute("user", user);
        model.addAttribute("ways", ways);
        model.addAttribute("parts", parts);
        model.addAttribute("favoriteSeqList", favoriteSeq);
        model.addAttribute("recipeTotal", TotalValue.getTotalCount());
        model.addAttribute("recipeList", recipePageResult);
        return "recipe/recipeList";
    }
}
```

#### Service
```java
public interface OpenRecipePageWithSearchService {
    RecipePageResult<Favorite, Object[]> searchTuplePageWithSortRecipes(Search search, PageRequest pageRequest);
}

public interface RecipeService {
    default List<RecipeManual> recipeManualSplit(OpenRecipe openRecipe) {
        List<RecipeManual> recipeManuals = new ArrayList<>();
    
        if (!openRecipe.getManual01().isEmpty()) {
            recipeManuals.add(new RecipeManual(openRecipe.getManual01()));
        }
        // ... 2 - 19까지 반복
        if (!openRecipe.getManual20().isEmpty()) {
            recipeManuals.add(new RecipeManual(openRecipe.getManual20()));
        }
    
        return recipeManuals;
    }
  
    default List<RecipeManualImg> recipeManualImgSplit(OpenRecipe openRecipe) {
        List<RecipeManualImg> recipeManualImages = new ArrayList<>();
    
        if (!openRecipe.getManualImg01().isEmpty()) {
            recipeManualImages.add(new RecipeManualImg(openRecipe.getManualImg01()));
        }
        // ... 2 - 19까지 반복
        if (!openRecipe.getManualImg20().isEmpty()) {
            recipeManualImages.add(new RecipeManualImg(openRecipe.getManualImg20()));
        }
    
        return recipeManualImages;
    }
  
    // 각각 조리 방법과 요리 분류 조회
    List<Way> recipeWayValueFound();
    List<Part> recipePartValueFound();
}

@Service
@RequiredArgsConstructor
public class RecipeAndSearchServiceImpl implements OpenRecipePageWithSearchService, RecipeService {
    private final OpenRecipePageWithSearchRepository openRecipePageWithSearchRepository;
    private final RecipeTupleRepository recipeTupleRepository;
  
    // 레시피 데이터를 조회할 때 당장 필요한 일부분만 가져옴 - 사진, 이름, 분류, 좋아요 개수 정도
    @Override
    public RecipePageResult<Favorite, Object[]> searchTuplePageWithSortRecipes(Search search, PageRequest pageRequest) {
        Page<Object[]> recipeDetails = openRecipePageWithSearchRepository.recipeSearchAndPageSeparateHandling(search, pageRequest);
        // Object[] 내 값 배열 5가지 값
        Function<Object[], Favorite> fn = (entity -> Favorite.builder().recipeId((Long) entity[0]).recipeMainImage((String) entity[1]).recipePart((String) entity[2]).recipeName((String) entity[3]).count((Long) entity[4]).build());
        return new RecipePageResult<>(recipeDetails, fn);
    }
  
    @Override
    public List<Way> recipeWayValueFound() {
        // 레시피의 조리 방식을 조회
        return recipeTupleRepository.recipeWayExtract().stream().map(Way::new).collect(Collectors.toList());
    }
  
    @Override
    public List<Part> recipePartValueFound() {
        // 레시피의 요리 분류를 조회
        return recipeTupleRepository.recipePartExtract().stream().map(Part::new).collect(Collectors.toList());
    }
}
```

#### Repository
```java
@Repository
public class RecipeTupleAndPageWithSearchRepositoryImpl extends QuerydslRepositorySupport implements OpenRecipePageWithSearchRepository, RecipeTupleRepository {
    @PersistenceContext
    private EntityManager entityManager;
  
    private final QOpenRecipeEntity openRecipeEntity = QOpenRecipeEntity.openRecipeEntity;
    private final QFavoriteEntity favoriteEntity = QFavoriteEntity.favoriteEntity;
  
    // 레시피 목록의 데이터를 가져옴, 목록에 표시되는 제한적인 데이터만 가져옴
    @Override
    public Page<Object[]> recipeSearchAndPageSeparateHandling(Search searchKeywords, Pageable pageable) {
        NumberPath<Long> aliasRecipe = Expressions.numberPath(Long.class, "id");
        JPAQuery<Tuple> tupleJPAQuery = separateSelectInit(aliasRecipe);
        tupleJPAQuery.where(searchAndQueryBuilder(searchKeywords));
        return tuplePagingWithSortHandler(tupleJPAQuery, aliasRecipe, pageable);
    }
  
    // 조리 방식 DB에서 조회
    @Override
    public List<String> recipeWayExtract() {
        JPAQuery<String> recipeWayList = jpaQuerySelectWayInit();
        recipeWayList.groupBy(openRecipeEntity.rcpWay2);
        return recipeWayList.fetch();
    }
  
    // 요리 분류 DB에서 조회
    @Override
    public List<String> recipePartExtract() {
        JPAQuery<String> recipeWayList = jpaQuerySelectPartInit();
        recipeWayList.groupBy(openRecipeEntity.rcpPat2);
        return recipeWayList.fetch();
    }
    // Group by를 통해 중복 생략
  
    // 데이터 개수 파악을 위해 group by
    private JPAQuery<Tuple> separateSelectInit(NumberPath<Long> aliasRecipe) {
        return jpaQuerySeparateStart(aliasRecipe).leftJoin(favoriteEntity).on(favoriteEntity.recipe.id.eq(openRecipeEntity.id)).groupBy(openRecipeEntity.id);
    }
  
    // 레시피의 일부분만 가져옴, 정렬을 위해 count를 NumberPath로 저장
    private JPAQuery<Tuple> jpaQuerySeparateStart(NumberPath<Long> aliasRecipe) {
        return jpaQueryOpenInit().from(openRecipeEntity).select(openRecipeEntity.id, openRecipeEntity.attFileNoMain, openRecipeEntity.rcpPat2, openRecipeEntity.rcpNm, favoriteEntity.recipe.id.count().as(aliasRecipe));
    }
  
    private JPAQuery<String> jpaQuerySelectWayInit() {
        return jpaQueryOpenInit().from(openRecipeEntity).select(openRecipeEntity.rcpWay2);
    }
  
    private JPAQuery<String> jpaQuerySelectPartInit() {
        return jpaQueryOpenInit().from(openRecipeEntity).select(openRecipeEntity.rcpPat2);
    }
  
    private JPAQuery<OpenRecipeEntity> jpaQueryOpenInit() {
        return new JPAQuery<>(entityManager);
    }
  
    private void pageSortSetting(JPQLQuery<Tuple> query, NumberPath<Long> aliasRecipe, Sort pageSort) {
        pageSort.stream().forEach(order -> {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            String prop = order.getProperty();
            PathBuilder orderByExpression = new PathBuilder(OpenRecipeEntity.class, "openRecipeEntity");
            // 정렬의 기준 확인
            if (prop.equals("favorite")) {
                // NumberPath를 기준으로 
                query.orderBy(aliasRecipe.desc());
            } else {
                query.orderBy(new OrderSpecifier(direction, orderByExpression.get(prop)));
            }
        });
    }
  
    private List<Tuple> sqlTuplePageSetting(JPAQuery<Tuple> openAPIDataHandle, Pageable pageable) {
        openAPIDataHandle.offset(pageable.getOffset()).limit(pageable.getPageSize());
        return openAPIDataHandle.fetch();
    }
}
```

#### RecipePageResult
```java
@Getter
@Setter
public class RecipePageResult<DTO, EN> extends PageResult<DTO, EN> {
    private List<List<DTO>> divideList = new ArrayList<>();
    /*
     * Recipe page rule
     * start page : 1
     * end page : total count / page size : (in page element count) + if (total count % page size : (in page element count) > 0) + 1
     *   ex) 1061 / 10 == 106 + 1 == 107 page
     * one page : total page size set = n, n is set by param
     * */

    // 데이터를 매 줄마다 몇개의 단위로 끊어서 나타낼지 정함 
    private int interval;

    private int sizeOfPage;
    private int numberOfPage;
    private int startPage, endPage;
    private boolean prevPage, nextPage;

    private List<Integer> pageList;

    public RecipePageResult(Page<EN> result, Function<EN, DTO> fn) {
        super(result, fn);
        this.interval = 4;
        divideDTOList();
    }

    private void divideDTOList() {
        int max = super.getDtoList().size();
        for (int i = 0; i < max; i += this.interval) {
            int iInterval = i + this.interval;
            if (iInterval > max) {
                iInterval = max--;
            }
            this.divideList.add(super.getDtoList().subList(i, iInterval));
        }
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    @Override
    public void makePageList(Pageable pageable) {
        setPage(pageable);
        boolean isLast = super.getNowPage() == super.getTotalPage();
        super.setLastPage(isLast);

        double pageDouble = (double) sizeOfPage;
        int tempEnd = (int)(Math.ceil(super.getNowPage()/pageDouble)) * sizeOfPage;

        startPage = tempEnd - (sizeOfPage - 1);
        endPage = Math.min(super.getTotalPage(), tempEnd);
        prevPage = startPage > 1;
        nextPage = super.getTotalPage() > tempEnd;

        super.setFirstPage(prevPage);
        super.setLastPage(nextPage);

        pageList = IntStream.rangeClosed(startPage, endPage).boxed().collect(Collectors.toList());
    }

    @Override
    public void setPage(Pageable pageable) {
        super.setNowPage(pageable.getPageNumber() + 1);

        this.numberOfPage = 10;
        this.sizeOfPage = this.numberOfPage;
    }
}
```

#### 레시피 데이터의 조회를 쉽게 카테고리 설정 검색
레시피의 데이터를 일일이 찾으면 힘듭니다. 카테고리에서 원하는 레시피의 재료나 조리방식, 요리 분류, 이름 등을 통해 원하는 레시피를 찾을 수 있습니다.
데이터가 없는 경우 아예 표시되지 않습니다. 레시피를 좋아요가 많은순, 일반적인 순서로 정렬할 수 있습니다.

## 레시피 상세정보
![](recipe/img/레시피_상세보기_비로그인.png)
<div style="text-align: center;">레시피 상세보기</div>

![](recipe/img/레시피_상세보기_조리과정.png)
<div style="text-align: center;">레시피 상세보기 조리과정</div>

![](recipe/img/레시피_상세보기_관련_카테고리추천.png)
<div style="text-align: center;">레시피 상세와 관련있는 레시피 추천</div>

#### recipeDetail.html
```html
<!-- Ajax를 활용해 좋아요 구현 -->
<script th:inline="javascript">
    /*<![CDATA[*/
    
    // 좋아요 시 해당 Ajax 함수가 실행, Post 요청으로 좋아요 추가, 삭제
    function favoriteSend() {
        // form 내의 input 태그 값을 가져옴
        const user =$("#user").val();
        const recipeId =$("#recipeId").val();
        const FavoriteRecipe = {
            id : null,
            recipeId : recipeId,
            userEmail : user
        };
        $.ajax({
            url: "/api/v1/ajax/favorite",
            data: FavoriteRecipe,
            type: 'POST',
        }).done(function (data){
            $("#favoriteCheck").replaceWith(data);
        });
    }

    // 비로그인 사용자에게 로그인 후 이용 가능하다는 알림창 전달
    function noLogin() {
        alert("로그인 후 좋아요를 할 수 있습니다.");
    }
    /*]]>*/
</script>

<!-- 해당 form이 로그인 한 경우만 동작, 숨겨진 값에 따라 좋아요를 설정 -->
<form th:if="${ user != null }">
  <!-- Ajax에서 데이터를 읽어올때 이 input에서 가져옴 -->
  <input type="hidden" id="recipeId" th:value="${ recipe.id }">
  <input type="hidden" id="recipeSeq" th:value="${ recipe.rcpSeq }">
  <input type="hidden" id="user" th:value="${ user.email }">
  <div id="favoriteCheck">
    <button type="button" onclick="favoriteSend()" rel="tooltip" th:class="'btn btn-light btn-link' + ${ isFavorite ? ' btn-rose' : ' btn-default' }">
      <i class="material-icons">favorite</i>
    </button>
  </div>
</form>
<form th:unless="${ user != null }">
  <div id="noFavoriteCheck">
    <!-- 비로그인자는 좋아요를 누를 수 없음 -->
    <button type="button" onclick="noLogin()" rel="tooltip" class="btn btn-light btn-link btn-default">
      <i class="material-icons">favorite</i>
    </button>
  </div>
</form>
```

좋아요를 Ajax로 구현, 비동기식으로 앱에서 동일 계정으로 로그인한채로 좋아요를 취소하거나 누를경우 바로 반영됨

#### Controller
```java
@RequestMapping("/recipes")
@RequiredArgsConstructor
@Controller
public class RecipeController {
    private final RecipeRecommendService recipeRecommendService;
    private final OpenRecipeService openRecipeService;
    private final RecipeService recipeService;
    private final FavoriteService favoriteService;

    // 레시피 상세보기
    @GetMapping("/detail/{id}")
    public String detailRecipe(@PathVariable Long id, @LoginSession SessionUser user, Model model) {
        boolean isFavorite = false;
        // 레시피 존재 확인
        OpenRecipe recipe = openRecipeService.findRecipe(id);

        if (user != null) {
            // 좋아요한 레시피인지 확인
            Favorite recipeFavorite = favoriteService.findRecipe(recipe.getRcpSeq(), user.getEmail());
            
            // 처음으로 레시피 상세보기로 진입하면 좋아요 상태인지 확인
            if (recipeFavorite.getId() > 0L) {
                isFavorite = true;
            }
        }

        List<RecipeManual> recipeManuals = recipeService.recipeManualSplit(recipe);
        List<RecipeManualImg> recipeManualImages = recipeService.recipeManualImgSplit(recipe);

        Search part = Search.builder().part(recipe.getRcpPat2()).build();
        
        // 하단의 해당 레시피와 관련된 무작위 레시피 4가지 추천
        List<Recommend> recommends = recommendRecipeList(part);

        model.addAttribute("user", user);
        model.addAttribute("recipe", recipe);
        model.addAttribute("isFavorite", isFavorite);
        model.addAttribute("recipeManuals", recipeManuals);
        model.addAttribute("recipeManualImages", recipeManualImages);
        model.addAttribute("relativeList", recommends);
        return "recipe/recipeDetail";
    }

    // 추천 레시피 조회
    private List<Recommend> recommendRecipeList(Search search) {
        return recipeRecommendService.findRecommendRecipe(search);
    }
}

// Ajax를 위한 좋아요 API
@RequestMapping("/api/v1/ajax")
@RequiredArgsConstructor
@Controller
public class FavoriteAPIController {
    private final UserService userService;
    private final FavoriteService favoriteService;
    private final OpenRecipeService openRecipeService;
  
    @PostMapping("/favorite")
    public String webOneFavoriteRecipe(Favorite favorite, Model model) {
        boolean isFavorite = false;
    
        // 유저의 이메일이 사용자 DB에 있는지 확인
        if (userService.isUserExist(favorite.getUserEmail())) {
            OpenRecipe recipe = openRecipeService.findRecipe(favorite.getRecipeId());
            Favorite find = favoriteService.findRecipe(recipe.getRcpSeq(), favorite.getUserEmail());
            
            // 좋아요 상태가 아니면 좋아요로
            if (find.getId() == 0L) {
                Favorite saved = favoriteService.create(favoriteRecipeValue(favorite, recipe));
                isFavorite = true;
            } else {
                // 좋아요 상태면 삭제
                favoriteService.delete(find);
            }
        }
    
        model.addAttribute("isFavorite", isFavorite);
        return "recipe/recipeDetail :: #favoriteCheck";
    }
  
    // 해당 값이 추가되기 전에 레시피의 값과 동일하게 설정
    private Favorite favoriteRecipeValue(Favorite favorite, OpenRecipe openRecipe) {
        favorite.setRecipeId(openRecipe.getId());
        favorite.setRecipeSeq(openRecipe.getRcpSeq());
        favorite.setRecipeName(openRecipe.getRcpNm());
        favorite.setRecipeMainImage(openRecipe.getAttFileNoMain());
        favorite.setRecipeWay(openRecipe.getRcpWay2());
        favorite.setRecipePart(openRecipe.getRcpPat2());
    
        return favorite;
    }
}
```

#### Service
````java
// 조건에 맞는 추천 레시피 조회
public interface RecipeRecommendService {
    List<Recommend> findRecommendRecipe(Search search);
}

// RecipeService 위의 목록과 동일, 생략
@Service
@RequiredArgsConstructor
public class RecipeAndSearchServiceImpl implements OpenRecipeService, RecipeRecommendService {
    private final RecipeTupleRepository recipeTupleRepository;

    @Override
    public List<Recommend> findRecommendRecipe(Search search) {
        List<Object[]> objects = recipeTupleRepository.sameRecommendRecipe(search);
        return objects.stream().map(objs -> Recommend.builder().id((Long) objs[0]).image((String) objs[1]).name((String) objs[2]).build()).collect(Collectors.toList());
    }
}

@RequiredArgsConstructor
@Service
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
  
    @Override
    public Favorite findRecipe(Long recipeSeq, String email) {
        Favorite favorite = Favorite.builder().id(0L).build();
    
        Object[] foundRawValue = favoriteRepository.findFavoriteByRecipeSeqAndEmail(recipeSeq, email);
        if (foundRawValue != null) {
            FavoriteEntity favoriteEntity = (FavoriteEntity) foundRawValue[0];
            OpenRecipeEntity openRecipeEntity = (OpenRecipeEntity) foundRawValue[1];
            favorite = entityToDto(favoriteEntity, openRecipeEntity);
        }
        return favorite;
    }
}
````

#### Repository
```java
@Repository
public class RecipeTupleAndPageWithSearchRepositoryImpl extends QuerydslRepositorySupport implements RecipeTupleRepository {  
    private final QOpenRecipeEntity openRecipeEntity = QOpenRecipeEntity.openRecipeEntity;
  
    // 동일한 카테고리 (요리 분류, 조리 방식)을 가진 레시피 무작위 4개 조회
    @Override
    public List<Object[]> sameRecommendRecipe(Search search) {
        JPAQuery<Tuple> recipeRecommend = jpaQuerySelectRecommendRandInit();
        recipeRecommend.where(searchAndQueryBuilder(search));
        // 4가지 무작위 데이터를 조회 NumberExpression의 random을 이용해 무작위 데이터 조회
        recipeRecommend.orderBy(NumberExpression.random().asc()).limit(4);
        List<Tuple> fetch = recipeRecommend.fetch();
        return fetch.stream().map(Tuple::toArray).collect(Collectors.toList());
    }
  
    private JPAQuery<Tuple> jpaQuerySelectRecommendRandInit() {
        // 레시피를 표시할 때 필요한 데이터만 선정해 조회
        return jpaQueryMySqlTemplateInit().from(openRecipeEntity).select(openRecipeEntity.id, openRecipeEntity.attFileNoMain, openRecipeEntity.rcpNm);
    }
    
    // 동일한 where 구문 생략
}

@Repository
public class FavoriteRepositoryImpl extends QuerydslRepositorySupport implements FavoriteRepository {
    private final QFavoriteEntity favoriteEntity = QFavoriteEntity.favoriteEntity;
  
    // 해당 이메일의 사용자가 같은 id를 가진 레시피에 좋아요를 한적 있는지 조회
    @Override
    public Object[] findFavoriteByRecipeSeqAndEmail(Long recipeSeq, String email) {
        JPQLQuery<Tuple> selectFavoriteRecipe = selectFavoriteRecipe(from(favoriteEntity));
        selectFavoriteRecipe.where(favoriteEntity.recipe.rcpSeq.eq(recipeSeq).and(favoriteEntity.userEmail.eq(email)));
        Tuple tuple = selectFavoriteRecipe.fetchFirst();
    
        if (tuple != null) {
            return tuple.toArray();
        }
        return null;
    }
}
```

#### 레시피 상세보기
레시피의 조리 과정과 영양소 정보, 하단에는 카테고리와 관련있는 무작위 4가지 레시피를 추천합니다.

## 나의 정보 좋아요 레시피
![](recipe/img/레시피_사용자.png)
<div style="text-align: center;">레시피 상세보기</div>

#### Controller
```java
@RequestMapping("/cookers")
@RequiredArgsConstructor
@Controller
public class UserController {
    private final FavoriteRankService favoriteRankService;
    private final SearchWithPageHandler<Favorite> searchWithPageHandler;
    private final UserService userService;

    @GetMapping("/info")
    public String userInfo(Model model, @LoginSession SessionUser user) {
        if (user != null) {
            User foundUser = userService.findByEmail(user.getEmail());
            // 사용자의 모든 좋아요 레시피 조회
            List<Favorite> favorites = favoriteRankService.usersFavoriteRecipe(foundUser.getEmail());
            // 좋아요 레시피 나열을 위해 4개로 여러줄로 나눔
            List<List<Favorite>> lists = searchWithPageHandler.pageRowRank(favorites);

            model.addAttribute("user", foundUser);
            model.addAttribute("usersFavorite", lists);
            return "user/userInfo";
        }
        return "index";
    }
}
```

#### Service
```java
@RequiredArgsConstructor
@Service
public class FavoriteServiceImpl implements FavoriteRankService {
    private final FavoriteRankRepository favoriteRankRepository;

    @Override
    public List<Favorite> usersFavoriteRecipe(String email) {
        List<Object[]> allFavorite = favoriteRankRepository.findRankFavoriteRecipeByEmail(email);
        return valueNotFoundCheck(allFavorite);
    }
}
```

#### Repository
````java
@Repository
public class FavoriteRepositoryImpl extends QuerydslRepositorySupport implements FavoriteRankRepository {
    private final QFavoriteEntity favoriteEntity = QFavoriteEntity.favoriteEntity;

    @Override
    public List<Object[]> findRankFavoriteRecipeByEmail(String email) {
        // 사용자의 좋아요 데이터 반환, 좋아요 데이터와 레시피 데이터, 좋아요 개수를 가져옴
        JPQLQuery<Tuple> selectFavoriteRecipe = selectFavoriteRecipeWithRank(from(favoriteEntity));
        return whereFavoriteRecipe(selectFavoriteRecipe, email).fetch().stream().map(Tuple::toArray).collect(Collectors.toList());
    }

    // left join을 통해 레시피 데이터 사용
    private JPQLQuery<Tuple> selectFavoriteRecipeWithRank(JPQLQuery<FavoriteEntity> query) {
        return query.select(favoriteEntity, openRecipeEntity, favoriteEntity.recipe.count())
                .leftJoin(openRecipeEntity).on(favoriteEntity.recipe.id.eq(openRecipeEntity.id))
                .from(favoriteEntity);
    }

    // 검색 시 조건 설정 후 count를 위해 group by
    private JPQLQuery<Tuple> whereFavoriteRecipe(JPQLQuery<Tuple> selectQuery, String email) {
        return selectQuery.where(favoriteEntity.userEmail.eq(email)).groupBy(favoriteEntity.recipe.rcpSeq);
    }
}
````
