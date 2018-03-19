package io.mockk.agent.android;

interface MockMaker {
    <T> T createMock(MockCreationSettings<T> settings, MockHandler handler);

    void resetMock(Object mock, MockHandler newHandler, MockCreationSettings settings);

    TypeMockability isTypeMockable(Class<?> type);

    MockHandler getHandler(Object mock);
}
