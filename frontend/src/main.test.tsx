import { screen } from '@testing-library/react';
import { expect, it } from 'vitest';
import { fakeApi } from './test/fakeApi';
import { serviceRoutes } from './test/fixtures';

it('renders the app into the root element', async () => {
  fakeApi(serviceRoutes());
  document.body.innerHTML = '<div id="root"></div>';

  await import('./main');

  expect(await screen.findByRole('heading', { level: 1, name: 'Kafka Payments Demo' })).toBeInTheDocument();
});
