package com.omnisync.core.connector;

import com.omnisync.core.auth.AuthStrategy;
import com.omnisync.core.error.AuthenticationException;
import com.omnisync.core.error.OmniSyncException;
import com.omnisync.core.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConnectorTest {

    @Mock
    private AuthStrategy authStrategy;

    static class TestRecord {
        private final String id;
        TestRecord(String id) { this.id = id; }
        public String getId() { return id; }
    }

    static class StubConnector extends BaseConnector<TestRecord> {
        private int callCount = 0;

        StubConnector(AuthStrategy authStrategy) {
            super(authStrategy);
        }

        @Override
        public Page<TestRecord> fetchPage(String cursor) {
            callCount++;
            if (cursor == null) {
                return new Page<>(List.of(new TestRecord("r1"), new TestRecord("r2")), "c2", true);
            }
            return Page.lastPage(List.of(new TestRecord("r3")));
        }

        public int getCallCount() {
            return callCount;
        }
    }

    @Test
    @DisplayName("BaseConnector delegates authenticate to AuthStrategy")
    void delegatesAuthentication() {
        StubConnector connector = new StubConnector(authStrategy);
        connector.authenticate();
        verify(authStrategy).authenticate();
        assertThat(connector.getAuthStrategy()).isSameAs(authStrategy);
    }

    @Test
    @DisplayName("BaseConnector propagates authentication errors")
    void propagatesAuthenticationErrors() {
        doThrow(new AuthenticationException("invalid token")).when(authStrategy).authenticate();
        StubConnector connector = new StubConnector(authStrategy);

        assertThatThrownBy(connector::authenticate)
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("invalid token");
    }

    @Test
    @DisplayName("BaseConnector extractRecords returns records or empty list")
    void extractRecordsSafely() {
        StubConnector connector = new StubConnector(authStrategy);
        Page<TestRecord> page = new Page<>(List.of(new TestRecord("x")), null, false);
        assertThat(connector.extractRecords(page)).hasSize(1);
        assertThat(connector.extractRecords(null)).isEmpty();
    }

    @Test
    @DisplayName("BaseConnector paginate streams through all pages")
    void paginateStreamsRecords() {
        StubConnector connector = new StubConnector(authStrategy);
        List<String> ids = new ArrayList<>();
        for (TestRecord r : connector.paginate()) {
            ids.add(r.getId());
        }

        assertThat(ids).containsExactly("r1", "r2", "r3");
        assertThat(connector.getCallCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("BaseConnector handleError throws the exception")
    void handleErrorThrows() {
        StubConnector connector = new StubConnector(authStrategy);
        OmniSyncException error = new OmniSyncException("upstream error");

        assertThatThrownBy(() -> connector.handleError(error))
                .isSameAs(error);
    }

    @Test
    @DisplayName("BaseConnector rejects null AuthStrategy")
    void rejectsNullAuthStrategy() {
        assertThatThrownBy(() -> new StubConnector(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("authStrategy must not be null");
    }
}
