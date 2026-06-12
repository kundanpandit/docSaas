import { Provider } from "react-redux";
import { store } from "@/app/store/store";
import AuthInitializer from "@/components/auth/AuthInitializer";
import ThemeInitializer from "@/features/theme/components/ThemeInitializer";

const ReduxProvider = ({ children }) => {
  return <Provider store={store}>
  <ThemeInitializer>
    <AuthInitializer>
      {children}
    </AuthInitializer>
  </ThemeInitializer>
</Provider>
};

export default ReduxProvider;